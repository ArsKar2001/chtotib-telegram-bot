package com.karmanchik.chtotibtelegrambot.bot;

import com.karmanchik.chtotibtelegrambot.bot.handler.Handler;
import com.karmanchik.chtotibtelegrambot.entity.User;
import com.karmanchik.chtotibtelegrambot.entity.enums.BotState;
import com.karmanchik.chtotibtelegrambot.entity.enums.Role;
import com.karmanchik.chtotibtelegrambot.entity.enums.UserState;
import com.karmanchik.chtotibtelegrambot.exception.ResourceNotFoundException;
import com.karmanchik.chtotibtelegrambot.jpa.JpaUserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Log4j2
@Component
public class UpdateReceiver {
    private final List<Handler> handlers;
    private final JpaUserRepository userRepository;

    public UpdateReceiver(List<Handler> handlers, JpaUserRepository userRepository) {
        this.handlers = handlers;
        this.userRepository = userRepository;
    }

    public List<PartialBotApiMethod<? extends Serializable>> handle(Update update) {
        try {
            if (isMessageWithText(update)) {
                Message message = update.getMessage();
                Integer chatId = message.getFrom().getId();
                String userName = message.getChat().getUserName();
                User user = userRepository.findByChatIdAndUserName(chatId, userName)
                        .orElseGet(() -> userRepository.save(User
                                .builder(chatId, userName)
                                .botState(BotState.START)
                                .userState(UserState.NONE)
                                .role(Role.NONE)
                                .build()));
                log.info("User - {}", user);
                Handler handler = getHandlerByUser(user);
                log.info("Handler - {}", handler.getName());
                return handler.handle(user, message.getText());
            } else if (update.hasCallbackQuery()) {
                CallbackQuery callbackQuery = update.getCallbackQuery();
                Integer chatId = callbackQuery.getFrom().getId();
                String userName = callbackQuery.getMessage().getChat().getUserName();
                User user = userRepository.findByChatIdAndUserName(chatId, userName)
                        .orElseGet(() -> userRepository.save(User
                                .builder(chatId, userName)
                                .botState(BotState.START)
                                .userState(UserState.NONE)
                                .role(Role.NONE)
                                .build()));
                log.info("User - {}", user);
                Handler handler = getHandlerByUser(user);
                log.info("Handler - {}", handler.getName());
                return handler.handle(user, callbackQuery.getData());
            }
            throw new UnsupportedOperationException();
        } catch (UnsupportedOperationException | ResourceNotFoundException e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private Handler getHandlerByUser(User user) {
        return handlers.stream()
                .filter(handler -> handler.operatedBotState() == user.getBotState())
                .filter(handler -> handler.operatedUserRoles().stream()
                        .anyMatch(role -> role == user.getRole()))
                .filter(handler -> handler.operatedUserSate().stream()
                        .anyMatch(state -> state == user.getUserState()))
                .findAny()
                .orElseThrow(UnsupportedOperationException::new);
    }

    private boolean isMessageWithText(Update update) {
        return !update.hasCallbackQuery() && update.hasMessage() && update.getMessage().hasText();
    }
}
