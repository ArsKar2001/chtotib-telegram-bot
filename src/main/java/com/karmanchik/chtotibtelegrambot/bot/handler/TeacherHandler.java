package com.karmanchik.chtotibtelegrambot.bot.handler;

import com.karmanchik.chtotibtelegrambot.bot.handler.helper.DateHelper;
import com.karmanchik.chtotibtelegrambot.bot.handler.helper.HandlerHelper;
import com.karmanchik.chtotibtelegrambot.bot.handler.helper.Helper;
import com.karmanchik.chtotibtelegrambot.bot.util.TelegramUtil;
import com.karmanchik.chtotibtelegrambot.entity.Lesson;
import com.karmanchik.chtotibtelegrambot.entity.Replacement;
import com.karmanchik.chtotibtelegrambot.entity.Teacher;
import com.karmanchik.chtotibtelegrambot.entity.User;
import com.karmanchik.chtotibtelegrambot.entity.enums.BotState;
import com.karmanchik.chtotibtelegrambot.entity.enums.Role;
import com.karmanchik.chtotibtelegrambot.entity.enums.UserState;
import com.karmanchik.chtotibtelegrambot.entity.enums.WeekType;
import com.karmanchik.chtotibtelegrambot.jpa.JpaLessonsRepository;
import com.karmanchik.chtotibtelegrambot.jpa.JpaReplacementRepository;
import com.karmanchik.chtotibtelegrambot.jpa.JpaTeacherRepository;
import com.karmanchik.chtotibtelegrambot.jpa.JpaUserRepository;
import com.karmanchik.chtotibtelegrambot.model.NumberLesson;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.function.Consumer;

import static com.karmanchik.chtotibtelegrambot.bot.handler.constants.ConstantsHandler.MESSAGE_SPLIT;

@Log4j2
@Component
@RequiredArgsConstructor
public class TeacherHandler extends MainHandler {
    private final JpaUserRepository userRepository;
    private final JpaTeacherRepository teacherRepository;
    private final JpaLessonsRepository lessonsRepository;
    private final JpaReplacementRepository replacementRepository;

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> getTimetableNextDay(User user) {
        Teacher teacher = teacherRepository.findByUser(user)
                .orElseThrow();
        LocalDate date = DateHelper.getNextSchoolDate();
        WeekType weekType = DateHelper.getWeekType();
        String name = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Helper.getLocale());

        List<Lesson> lessons = lessonsRepository.findByTeacherOrderByPairNumberAsc(teacher);
        List<Replacement> replacements = replacementRepository.findByTeacherOrderByDateAscPairNumberAsc(teacher);

        StringBuilder message = new StringBuilder();
        if (!lessons.isEmpty()) {
            message.append(MESSAGE_SPLIT).append("\n")
                    .append("Расписание ").append("<b>").append(teacher.getName()).append("</b>").append(" на ").append("<b>").append(date).append("</b>").append(" (").append(name).append("):").append("\n")
                    .append(MESSAGE_SPLIT).append("\n");
            lessons.stream()
                    .filter(lesson -> lesson.getDay() == date.getDayOfWeek().getValue())
                    .filter(lesson -> lesson.getWeekType() == weekType || lesson.getWeekType() == WeekType.NONE)
                    .forEach(getLessonConsumer(message));
        } else {
            message.append("Пар на ").append("<b>").append(date).append("</b>").append(" нет.");
        }
        if (!replacements.isEmpty()) {
            message.append(MESSAGE_SPLIT).append("\n")
                    .append("Замена:").append("\n")
                    .append(MESSAGE_SPLIT).append("\n");
            replacements.forEach(replacement -> message.append("\t")
                    .append(replacement.getDate()).append("\t<b>|</b>\t")
                    .append(replacement.getPairNumber()).append("\t<b>|</b>\t")
                    .append(replacement.getDiscipline()).append("\t<b>|</b>\t")
                    .append(replacement.getAuditorium()).append("\t<b>|</b>\t")
                    .append(replacement.getGroup().getName())
                    .append("\n"));
        } else {
            message.append("Замены на ").append("<b>").append(date).append("</b>").append(" нет.");
        }
        return List.of(TelegramUtil.createMessageTemplate(user)
                        .setText(message.toString()),
                HandlerHelper.mainMessage(user)
        );
    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> getTimetableFull(User user) {

        Teacher teacher = teacherRepository.findByUser(user)
                .orElseThrow();
        WeekType weekType = DateHelper.getWeekType();
        StringBuilder message = new StringBuilder();

        List<Lesson> lessons = lessonsRepository.findByTeacherOrderByPairNumberAsc(teacher);
        message.append("Расписание ").append("<b>").append(teacher.getName()).append("</b>:").append("\n");
        lessons.stream()
                .map(Lesson::getDay)
                .distinct()
                .forEach(day -> {
                    String displayName = DayOfWeek.of(day).getDisplayName(TextStyle.FULL, Helper.getLocale());
                    message.append(MESSAGE_SPLIT).append("\n")
                            .append("<b>").append(displayName).append("</b>:").append("\n");
                    lessons.stream()
                            .filter(lesson -> lesson.getDay().equals(day))
                            .filter(lesson -> lesson.getWeekType() == weekType || lesson.getWeekType() == WeekType.NONE)
                            .forEach(getLessonConsumer(message));
                });
        return List.of(TelegramUtil.createMessageTemplate(user)
                        .setText(message.toString()),
                HandlerHelper.mainMessage(user)
        );
    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> editProfile(User user) {
        user.setBotState(BotState.REG);
        user.setUserState(UserState.SELECT_ROLE);
        return List.of(
                HandlerHelper.selectRole(userRepository.save(user)));
    }

    private Consumer<Lesson> getLessonConsumer(StringBuilder message) {
        return lesson -> {
            String number = NumberLesson.get(lesson.getPairNumber());
            message.append("\t")
                    .append(number).append("\t<b>|</b>\t")
                    .append(lesson.getDiscipline()).append("\t<b>|</b>\t")
                    .append(lesson.getAuditorium()).append("\t<b>|</b>\t")
                    .append(lesson.getGroup().getName())
                    .append("\n");
        };
    }

    @Override
    public BotState operatedBotState() {
        return BotState.AUTHORIZED;
    }

    @Override
    public List<Role> operatedUserRoles() {
        return List.of(Role.TEACHER);
    }

    @Override
    public List<UserState> operatedUserSate() {
        return List.of(
                UserState.NONE,
                UserState.SELECT_COURSE,
                UserState.SELECT_GROUP
        );
    }
}