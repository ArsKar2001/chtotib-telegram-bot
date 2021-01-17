package com.karmanchik.chtotibtelegrambot.bot;

import com.karmanchik.chtotibtelegrambot.bot.handler.Handler;
import com.karmanchik.chtotibtelegrambot.entity.User;
import com.karmanchik.chtotibtelegrambot.model.State;
import com.karmanchik.chtotibtelegrambot.repository.JpaUserRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

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
                final Message message = update.getMessage();
                final int chatId = message.getFrom().getId();
                final int messageId = message.getMessageId();
                final User user = userRepository.findByChatId(chatId)
                        .orElseGet(() -> userRepository.save(new User(chatId)));
                user.setBotLastMessageId(messageId);
                userRepository.save(user);
                return getHandlerByState(user).handle(user, message.getText());
            } else if (update.hasCallbackQuery()) {
                final CallbackQuery callbackQuery = update.getCallbackQuery();
                final int chatId = callbackQuery.getFrom().getId();
                final int messageId = callbackQuery.getMessage().getMessageId();
                final User user = userRepository.findByChatId(chatId)
                        .orElseGet(() -> userRepository.save(new User(chatId)));
                user.setBotLastMessageId(messageId);
                userRepository.save(user);
                return getHandlerByState(user).handle(user, callbackQuery.getData());
            }
            throw new UnsupportedOperationException();
        } catch (UnsupportedOperationException e) {
            return Collections.emptyList();
        }
    }

    private Handler getHandlerByState(@NotBlank User user) {
        return handlers.stream()
                .filter(h -> h.operatedUserListState().stream()
                        .anyMatch(user.getUserState()::equals))
                .filter(h -> h.operatedBotState().equals(user.getBotState()))
                .findAny()
                .orElseThrow(UnsupportedOperationException::new);
    }

    private boolean isMessageWithText(Update update) {
        return !update.hasCallbackQuery() && update.hasMessage() && update.getMessage().hasText();
    }
}
