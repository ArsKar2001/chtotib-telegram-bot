package com.karmanchik.chtotibtelegrambot.bot.handler;

import com.karmanchik.chtotibtelegrambot.entity.*;
import com.karmanchik.chtotibtelegrambot.model.WeekType;
import com.karmanchik.chtotibtelegrambot.service.GroupService;
import com.karmanchik.chtotibtelegrambot.service.UserService;
import com.karmanchik.chtotibtelegrambot.util.TelegramUtil;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import static com.karmanchik.chtotibtelegrambot.util.TelegramUtil.DAYS_OF_WEEK;
import static com.karmanchik.chtotibtelegrambot.util.TelegramUtil.MainCommand.*;

@Log4j
@Component
public class MainHandler implements Handler {

    private final GroupService groupService;
    private final UserService userService;

    public MainHandler(GroupService groupService,
                       UserService userService) {
        this.groupService = groupService;
        this.userService = userService;
    }

    @Override
    public List<PartialBotApiMethod<? extends Serializable>> handle(User user, String message) {
        try {
            if (message.equalsIgnoreCase(COM_1.getValue())) {
                log.debug("!!!! log debug 1: select handler - getTimetableForTomorrow for " + user.toString());
                return getTimetableForTomorrow(user);
            } else if (message.equalsIgnoreCase(COM_2.getValue())) {
                log.debug("!!!! log debug 1: select handler - getFullTimetableForGroup for " + user.toString());
                return getFullTimetableForGroup(user);
            } else if (message.equalsIgnoreCase(COM_3.getValue())) {
                log.debug("!!!! log debug 1: select handler - getMessageInfo for " + user.toString());
                return getMessageInfo(user);
            } else if (message.equalsIgnoreCase(COM_4.getValue())) {
                log.debug("!!!! log debug 1: select handler - getEditProfile for " + user.toString());
                return getEditProfile(user);
            }
            log.debug("!!!! log debug 1: select handler - null for " + user.toString());
            return List.of(TelegramUtil.mainMessage(user));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return List.of(
                    TelegramUtil.createMessageTemplate(user)
                            .setText("Ошибка: " + e.getMessage() + ";" + e.getLocalizedMessage()),
                    TelegramUtil.mainMessage(user)
            );
        }
    }

    private List<PartialBotApiMethod<? extends Serializable>> getMessageInfo(User user) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("<b>ChatId</b>:\t").append(user.getChatId()).append("\n")
                .append("<b>Роль</b>:\t").append(user.getRole().getDescription()).append("\n");
        if (isStudent(user)) {
            Group group = user.getGroup();
            log.debug("!!!! log debug getMessageInfo: find group by id=" + group.getId() + " - " + group.toString());
            stringBuilder.append("<b>Группа</b>:\t").append(group.getGroupName()).append("\n");
        } else {
            stringBuilder.append("<b>Имя</b>:\t").append(user.getName()).append("\n");
        }
        stringBuilder.append("\n<b>От разработчика</b>:")
                .append("\nЭто не финальная версия моего бота. " +
                        "Сейчас он работает только с расписанием, однако в будущем будет учитывать замену и кидать новости с сайта <a href=\"https://www.chtotib.ru/\">ЧТОТиБ</a>. " +
                        "По всем ошибкам, вопросам или предложениям писать мне - @l_karmanchik_l.").append("\n");

        return List.of(
                TelegramUtil.createMessageTemplate(user)
                        .setText(stringBuilder.toString()),
                TelegramUtil.mainMessage(user)
        );
    }

    private boolean isStudent(User user) {
        return user.getRole().getNameRole().equalsIgnoreCase("STUDENT");
    }

    private List<PartialBotApiMethod<? extends Serializable>> getEditProfile(User user) {
        user.setRoleId(Role.Instance.NONE.getId());
        user.setName(null);
        user.setUserStateId(UserState.Instance.SELECT_ROLE.getId());
        user.setBotStateId(BotState.Instance.REG.getId());
        userService.save(user);
        return RegistrationHandler.selectRole(user);
    }

    private List<PartialBotApiMethod<? extends Serializable>> getFullTimetableForGroup(User user) {
        StringBuilder stringBuilder = new StringBuilder();
        log.debug("!!!! log debug getFullTimetableForGroup: create " + stringBuilder.getClass().toString());
        Calendar calendar = Calendar.getInstance();
        log.debug("!!!! log debug getFullTimetableForGroup: create " + calendar.getClass().toString() + " - " + calendar.toString());
        WeekType week = TelegramUtil.getWeekType(calendar);
        log.debug("!!!! log debug getFullTimetableForGroup: create " + week.getClass().toString() + " - " + week.toString());

        if (isStudent(user)) {
            Group group = user.getGroup();
            log.debug("!!!! log debug getTimetableForTomorrow: find group by id=" + group.getId() + " - " + group.toString());
            var dayList = groupService.getListDaysOfWeekByGroupName(group.getGroupName());
            log.debug("!!!! log debug getTimetableForTomorrow: find dayList by " + group.getGroupName() + " - " + Arrays.toString(dayList.toArray()));
            var lessons = groupService.getListLesson(group.getGroupName(), week.name());
            log.debug("!!!! log debug getTimetableForTomorrow: find lessons by " + group.getGroupName() + " - " + Arrays.toString(lessons.toArray()));

            stringBuilder.append("Неделя: ").append("<b>").append(week.getValue()).append("</b>").append("\n");
            stringBuilder.append("Расписание для группы ").append("<b>").append(group.getGroupName()).append("</b>").append(":\n");
            stringBuilder.append("\n").append(new String(new char[60]).replace('\0', '-')).append("\n");
            dayList.forEach(day -> {
                String dayOfWeek = DAYS_OF_WEEK.get(day);
                stringBuilder.append(dayOfWeek).append(":\n");
                lessons.forEach(lesson -> {
                    if (lesson.getDayOfWeek().equals(day)) {
                        stringBuilder.append("\t\t-\t").append(lesson.getLessonNumber()).
                                append("\t|\t").append(lesson.getDiscipline()).
                                append("\t|\t").append(lesson.getAuditorium()).
                                append("\t|\t").append(lesson.getTeacher())
                                .append("\n");
                    }
                });
                stringBuilder.append(new String(new char[60]).replace('\0', '-')).append("\n");
            });
        } else {
            var lessons = groupService.getListLesson(user.getName().toLowerCase(), week.name());
            log.debug("!!!! log debug getTimetableForTomorrow: find lessons by " + user.getName() + " - " + Arrays.toString(lessons.toArray()));
            var dayList = groupService.getListDaysOfWeekByTeacher(user.getName());
            log.debug("!!!! log debug getTimetableForTomorrow: find dayList by " + user.getName() + " - " + Arrays.toString(dayList.toArray()));
            stringBuilder.append("Расписание для педагога ").append("<b>").append(user.getName()).append("</b>").append(":\n");
            stringBuilder.append(new String(new char[60]).replace('\0', '-')).append("\n");
            dayList.forEach(day -> {
                stringBuilder.append(DAYS_OF_WEEK.get(day)).append(":\n");
                lessons.forEach(lesson -> {
                    if (lesson.getDayOfWeek().equals(day)) {
                        stringBuilder.append("\t\t-\t").append(lesson.getLessonNumber()).
                                append("\t|\t").append(lesson.getGroupName()).
                                append("\t|\t").append(lesson.getDiscipline()).
                                append("\t|\t").append(lesson.getAuditorium()).
                                append("\t|\t").append(lesson.getTeacher())
                                .append("\n");
                    }
                });
                stringBuilder.append(new String(new char[60]).replace('\0', '-')).append("\n");
            });
        }
        return List.of(
                TelegramUtil.createMessageTemplate(user)
                        .setText(stringBuilder.toString()),
                TelegramUtil.mainMessage(user)
        );
    }

    private List<PartialBotApiMethod<? extends Serializable>> getTimetableForTomorrow(User user) {
        Calendar next = TelegramUtil.getNextDate();
        log.debug("!!!! log debug getTimetableForTomorrow: create Calendar - " + next.toString());

        int nextDayOfWeek = TelegramUtil.getNextDayOfWeek();
        log.debug("!!!! log debug getTimetableForTomorrow: create nextDayOfWeek - " + nextDayOfWeek);
        WeekType weekType = TelegramUtil.getWeekType(next);
        log.debug("!!!! log debug getTimetableForTomorrow: create weekType - " + weekType.toString());

        StringBuilder stringBuilder = new StringBuilder();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");


        if (isStudent(user)) {
            Group group = user.getGroup();
            log.debug("!!!! log debug getTimetableForTomorrow: find group by id=" + group.getId() + " - " + group.toString());
            var lessonsForTomorrow = groupService
                    .findAllByGroupNameAndDayOfWeek(group.getGroupName(), nextDayOfWeek, weekType.name());
            log.debug("!!!! log debug getTimetableForTomorrow: find lessons by " + group.getGroupName() + " - " + Arrays.toString(lessonsForTomorrow.toArray()));
            String dayOfWeek = DAYS_OF_WEEK.get(nextDayOfWeek);
            stringBuilder.append("Расписание на <b>").append(dateFormat.format(next.getTime())).append("</b> (").append(dayOfWeek).append("):\n");
            stringBuilder.append(new String(new char[60]).replace('\0', '-')).append("\n");
            lessonsForTomorrow.forEach(lesson -> stringBuilder.append("\t\t-\t")
                    .append(lesson.getLessonNumber()).
                            append("\t|\t").append(lesson.getDiscipline()).
                            append("\t|\t").append(lesson.getAuditorium()).
                            append("\t|\t").append(lesson.getTeacher())
                    .append("\n"));
            stringBuilder.append(new String(new char[60]).replace('\0', '-')).append("\n");
            stringBuilder.append("Группа: <b>").append(group.getGroupName()).append("</b>\t\t\t\t").append("Неделя: ").append("<b>").append(weekType.getValue()).append("</b>");
        } else {
            var lessonsForTomorrow = groupService.findAllByTeacherAndDayOfWeek(user.getName().toLowerCase(), nextDayOfWeek, weekType.name());
            log.debug("!!!! log debug getTimetableForTomorrow: find lessons by " + user.getName() + " - " + Arrays.toString(lessonsForTomorrow.toArray()));
            String dayOfWeek = DAYS_OF_WEEK.get(nextDayOfWeek);
            stringBuilder.append("Расписание на <b>").append(dateFormat.format(next.getTime())).append("</b> (").append(dayOfWeek).append("):\n");
            stringBuilder.append("\n").append(new String(new char[60]).replace('\0', '-')).append("\n");
            lessonsForTomorrow.forEach(lesson -> stringBuilder.append("\t\t-\t").append(lesson.getLessonNumber()).
                    append("\t|\t").append(lesson.getGroupName()).
                    append("\t|\t").append(lesson.getDiscipline()).
                    append("\t|\t").append(lesson.getAuditorium()).
                    append("\t|\t").append(lesson.getTeacher())
                    .append("\n"));
            stringBuilder.append("\n").append(new String(new char[60]).replace('\0', '-')).append("\n");
            stringBuilder.append("Неделя: ").append("<b>").append(weekType.getValue()).append("</b>");
        }
        return List.of(
                TelegramUtil.createMessageTemplate(user)
                        .setText(stringBuilder.toString()),
                TelegramUtil.mainMessage(user)
        );
    }

    @Override
    public BotState.Instance operatedBotState() {
        return BotState.Instance.AUTHORIZED;
    }

    @Override
    public List<UserState.Instance> operatedUserListState() {
        return List.of(UserState.Instance.NONE);
    }
}
