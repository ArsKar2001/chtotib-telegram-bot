package com.karmanchik.chtotibtelegrambot.bot.handler;

import com.karmanchik.chtotibtelegrambot.bot.handler.constants.ConstantsHandler;
import com.karmanchik.chtotibtelegrambot.bot.handler.helper.HandlerHelperService;
import com.karmanchik.chtotibtelegrambot.bot.util.TelegramUtil;
import com.karmanchik.chtotibtelegrambot.entity.ChatUser;
import com.karmanchik.chtotibtelegrambot.entity.Group;
import com.karmanchik.chtotibtelegrambot.entity.Teacher;
import com.karmanchik.chtotibtelegrambot.entity.enums.BotState;
import com.karmanchik.chtotibtelegrambot.entity.enums.Role;
import com.karmanchik.chtotibtelegrambot.entity.enums.UserState;
import com.karmanchik.chtotibtelegrambot.exception.ResourceNotFoundException;
import com.karmanchik.chtotibtelegrambot.jpa.JpaChatUserRepository;
import com.karmanchik.chtotibtelegrambot.jpa.JpaGroupRepository;
import com.karmanchik.chtotibtelegrambot.jpa.JpaTeacherRepository;
import com.karmanchik.chtotibtelegrambot.model.Course;
import com.karmanchik.chtotibtelegrambot.model.IdTeacherName;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import static com.karmanchik.chtotibtelegrambot.bot.handler.constants.ConstantsHandler.*;

@Log4j2
@Component
@RequiredArgsConstructor
public class RegistrationHandler implements Handler {

    private final JpaChatUserRepository userRepository;
    private final JpaGroupRepository groupRepository;
    private final JpaTeacherRepository teacherRepository;

    private final HandlerHelperService helper;

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(ChatUser chatUser, String message) {
        try {
            UserState state = chatUser.getUserState();
            return switch (state) {
                case SELECT_COURSE -> helper.selectGroup(chatUser, message);
                case SELECT_GROUP -> selectGroupOrAccept(chatUser, message);
                case SELECT_ROLE -> switchRole(chatUser, message);
                case INPUT_TEXT -> selectTeacher(chatUser, message);
                case SELECT_TEACHER -> selectTeacherOrAccept(chatUser, message);
                default -> Collections.emptyList();
            };
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return List.of(
                    TelegramUtil.createMessageTemplate(chatUser)
                            .text("<b>Ошибка</b>: " + e.getMessage())
                            .build()
            );
        }
    }

    private List<PartialBotApiMethod<? extends Serializable>> switchRole(ChatUser chatUser, String message) {
        if (message.equalsIgnoreCase(ROLE_STUDENT)) {
            chatUser.setUserState(UserState.SELECT_COURSE);
            chatUser.setRole(Role.STUDENT);
            ChatUser save = userRepository.save(chatUser);
            return HandlerHelperService.createSelectCourseButtonPanel(save);
        } else if (message.equalsIgnoreCase(ROLE_TEACHER)) {
            chatUser.setRole(Role.TEACHER);
            return inputTeacherName(chatUser);
        }
        return Collections.emptyList();
    }

    private List<PartialBotApiMethod<? extends Serializable>> inputTeacherName(ChatUser chatUser) {
        chatUser.setUserState(UserState.INPUT_TEXT);
        return List.of(HandlerHelperService.inputMessage(
                userRepository.save(chatUser),
                "Введите фамилию..."));
    }

    private List<PartialBotApiMethod<? extends Serializable>> selectGroupOrAccept(ChatUser chatUser, String message) throws ResourceNotFoundException {
        if (Course.isCourse(message)) {
            return helper.selectGroup(chatUser, message);
        } else if (HandlerHelperService.isNumeric(message)) {
            int id = Integer.parseInt(message);
            log.info("Find group by id: {} ...", id);
            Group group = groupRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(id, Group.class));
            chatUser.setGroup(group);
            return List.of(
                    accept(chatUser)
            );
        }
        return Collections.emptyList();
    }

    private List<PartialBotApiMethod<? extends Serializable>> selectTeacherOrAccept(ChatUser chatUser, String message) throws ResourceNotFoundException {
        if (message.equalsIgnoreCase(CANCEL)) {
            return inputTeacherName(chatUser);
        } else if (HandlerHelperService.isNumeric(message)) {
            int id = Integer.parseInt(message);
            Teacher teacher = teacherRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException(id, Teacher.class));
            chatUser.setTeacher(teacher);
            return List.of(
                    accept(chatUser)
            );
        }
        return Collections.emptyList();
    }

    private List<PartialBotApiMethod<? extends Serializable>> selectTeacher(ChatUser chatUser, String message) {
        List<IdTeacherName> teacherNames = teacherRepository.findAllByName(message.toLowerCase());
        if (!teacherNames.isEmpty()) {
            chatUser.setUserState(UserState.SELECT_TEACHER);
            ChatUser save = userRepository.save(chatUser);

            return sendMessageItIsYou(save,
                    InlineKeyboardMarkup.builder()
                            .keyboard(TelegramUtil.createInlineKeyboardButtons(teacherNames, 2))
                            .build(),
                    TelegramUtil.createReplyKeyboardMarkup()
                            .keyboardRow(TelegramUtil.createKeyboardRow(List.of(ConstantsHandler.CANCEL)))
                            .oneTimeKeyboard(true)
                            .build()
            );
        }
        return sendMessageNotFound(chatUser);
    }


    private List<PartialBotApiMethod<? extends Serializable>> sendMessageItIsYou(ChatUser chatUser,
                                                                                 InlineKeyboardMarkup markup1,
                                                                                 ReplyKeyboardMarkup markup2) {
        return List.of(
                TelegramUtil.createMessageTemplate(chatUser)
                        .text("Выберите педагога:")
                        .replyMarkup(markup1)
                        .build(),
                TelegramUtil.createMessageTemplate(chatUser)
                        .text("...?")
                        .replyMarkup(markup2)
                        .build());
    }

    private List<PartialBotApiMethod<? extends Serializable>> sendMessageNotFound(ChatUser chatUser) {
        String outMessage = "Никого не нашел :(";
        return List.of(
                TelegramUtil.createMessageTemplate(chatUser)
                        .text(outMessage)
                        .build(),
                cancel(chatUser)
        );
    }

    private PartialBotApiMethod<? extends Serializable> accept(ChatUser chatUser) {
        chatUser.setUserState(UserState.NONE);
        chatUser.setBotState(BotState.AUTHORIZED);
        ChatUser save = userRepository.save(chatUser);
        return HandlerHelperService.mainMessage(save);
    }

    private PartialBotApiMethod<? extends Serializable> cancel(ChatUser chatUser) {
        chatUser.setUserState(UserState.SELECT_ROLE);
        chatUser.setBotState(BotState.REG);
        final ChatUser saveChatUser = userRepository.save(chatUser);
        return HandlerHelperService.selectRole(saveChatUser);
    }

    @Override
    public BotState operatedBotState() {
        return BotState.REG;
    }

    @Override
    public List<Role> operatedUserRoles() {
        return List.of(
                Role.STUDENT,
                Role.NONE,
                Role.TEACHER
        );
    }

    @Override
    public List<UserState> operatedUserSate() {
        return List.of(
                UserState.SELECT_ROLE,
                UserState.SELECT_COURSE,
                UserState.SELECT_TEACHER,
                UserState.SELECT_GROUP,
                UserState.INPUT_TEXT
        );
    }
}
