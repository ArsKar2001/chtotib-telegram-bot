package com.karmanchik.chtotibtelegrambot.bot.util;

import com.karmanchik.chtotibtelegrambot.entity.ChatUser;
import com.karmanchik.chtotibtelegrambot.model.BaseModel;
import com.karmanchik.chtotibtelegrambot.model.Course;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage.SendMessageBuilder;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup.ReplyKeyboardMarkupBuilder;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton.InlineKeyboardButtonBuilder;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TelegramUtil {
    private TelegramUtil() {
    }

    public static SendMessageBuilder createMessageTemplate(ChatUser chatUser) {
        String chatId = chatUser.getChatId().toString();
        return SendMessage.builder()
                .chatId(chatId)
                .parseMode(ParseMode.HTML);
    }

    public static ReplyKeyboardMarkupBuilder createReplyKeyboardMarkup() {
        return ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .selective(false);
    }

    public static KeyboardRow createKeyboardRow(List<String> values) {
        KeyboardRow row = new KeyboardRow();
        row.addAll(values);
        return row;
    }

    public static List<List<InlineKeyboardButton>> createInlineKeyboardButtons(List<? extends BaseModel> models, Integer countButtonInRow) {
        List<List<InlineKeyboardButton>> listList = new ArrayList<>();
        List<InlineKeyboardButton> buttonsLine = new ArrayList<>();

        models.stream()
                .map(model -> TelegramUtil.createInlineKeyboardButton(model.getName(), model.getId().toString()).build())
                .forEach(button -> {
                    buttonsLine.add(button);
                    if (buttonsLine.size() % countButtonInRow == 0) {
                        listList.add(new ArrayList<>(buttonsLine));
                        buttonsLine.clear();
                    }
                });
        listList.add(buttonsLine);
        return listList;
    }

    public static InlineKeyboardButtonBuilder createInlineKeyboardButton(String text, String command) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(command);
    }
}
