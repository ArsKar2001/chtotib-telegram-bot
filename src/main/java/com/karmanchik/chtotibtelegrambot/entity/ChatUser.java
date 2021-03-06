package com.karmanchik.chtotibtelegrambot.entity;

import com.karmanchik.chtotibtelegrambot.entity.enums.BotState;
import com.karmanchik.chtotibtelegrambot.entity.enums.Role;
import com.karmanchik.chtotibtelegrambot.entity.enums.UserState;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;


@Entity
@Table(name = "chat_users")
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatUser extends BaseEntity {
    @Column(name = "chat_id", unique = true)
    @NotNull
    private Long chatId;

    @Column(name = "user_name", unique = true)
    @NotNull
    private String userName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinTable(name = "user_teacher",
            joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)},
            inverseJoinColumns = {@JoinColumn(name = "teacher_id", referencedColumnName = "id", nullable = false)})
    private Teacher teacher = null;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinTable(name = "user_group",
            joinColumns = {@JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)},
            inverseJoinColumns = {@JoinColumn(name = "group_id", referencedColumnName = "id", nullable = false)})
    private Group group = null;


    @Column(name = "role_id")
    @NotNull
    private Role role = Role.NONE;

    @Column(name = "user_state_id")
    @NotNull
    private UserState userState = UserState.NONE;

    @Column(name = "bot_state_id")
    @NotNull
    private BotState botState = BotState.START;

    public static ChatUserBuilder builder(Long chatId, String userName) {
        return new ChatUserBuilder().chatId(chatId).userName(userName);
    }

    @Override
    public String toString() {
        return "ChatUser{" +
                "chatId=" + chatId +
                ", userName='" + userName + '\'' +
                ", role=" + role +
                ", userState=" + userState +
                ", botState=" + botState +
                ", id=" + id +
                '}';
    }
}

