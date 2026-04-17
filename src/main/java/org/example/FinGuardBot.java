package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.*;

public class FinGuardBot extends TelegramLongPollingBot {

    private enum State { START, SET_BALANCE, SET_RESERVE, SET_DAYS, WORK }
    private State currentState = State.START;

    private double balance, reserve;
    private int days;

    public FinGuardBot() {
        initDb();
        loadUserData();
    }

    @Override
    public String getBotUsername() { return "FinParent"; }

    @Override
    public String getBotToken() { return "8591173717:AAEeEF42bAacwHGW7v2HLvVzjowqhoWJtOk"; }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (text.equals("/start")) {
                sendText(chatId, "Привет! Я FinGuard. 🤖\nСколько у тебя сейчас денег всего?");
                currentState = State.SET_BALANCE;
                return;
            }

            if (text.equals("/help")) {
                sendHelp(chatId);
                return;
            }

            if (text.equalsIgnoreCase("/status")) {
                if (currentState == State.WORK) showStatus(chatId);
                else sendText(chatId, "Сначала нажми /start");
                return;
            }

            if (text.equalsIgnoreCase("/nextday")) {
                handleNextDay(chatId);
                return;
            }

            try {
                switch (currentState) {
                    case SET_BALANCE:
                        balance = Double.parseDouble(text);
                        sendText(chatId, "Принято. Какую сумму оставим как **Резерв**? 🛟");
                        currentState = State.SET_RESERVE;
                        break;
                    case SET_RESERVE:
                        reserve = Double.parseDouble(text);
                        sendText(chatId, "Через сколько дней следующий доход? 📅");
                        currentState = State.SET_DAYS;
                        break;
                    case SET_DAYS:
                        days = Integer.parseInt(text);
                        if (days <= 0) days = 1;
                        currentState = State.WORK;
                        saveUserData();
                        sendText(chatId, "✅ Настройка завершена!");
                        showStatus(chatId);
                        break;
                    case WORK:
                        processPurchase(chatId, Double.parseDouble(text));
                        break;
                }
            } catch (NumberFormatException e) {
                sendText(chatId, "❌ Ошибка! Жду число. Если запутался — /help");
            }
        }
    }

    private void handleNextDay(long chatId) {
        if (currentState != State.WORK) {
            sendText(chatId, "Сначала настрой бота через /start");
            return;
        }
        if (days > 1) {
            days--;
            saveUserData();
            sendText(chatId, "☀️ **Новый день настал!**\nДней до дохода: " + days);
            showStatus(chatId);
        } else {
            sendText(chatId, "🎉 Период закончен! Нажми /start для нового цикла.");
            currentState = State.START;
        }
    }

    private void processPurchase(long chatId, double price) {
        double available = balance - reserve;
        double limit = available / days;

        if (price <= limit) {
            balance -= price;
            saveUserData();
            sendText(chatId, "✅ **ОДОБРЕНО**\nЗаписал: -" + price + " сом.");
        } else if (price > limit * 1.5) {
            int riskDays = (int) (price / limit);
            sendText(chatId, "⛔ **ЗАБЛОКИРОВАНО**\nЛимит: " + String.format("%.2f", limit) + " сом.\nБез денег на " + riskDays + " дн. 😡");
        } else {
            balance -= price;
            saveUserData();
            sendText(chatId, "⚠️ **ВЗЯТО ИЗ БУДУЩЕГО**\nПревышение лимита. Завтра экономим!");
        }
    }

    private void showStatus(long chatId) {
        double available = balance - reserve;
        double limit = available / days;
        int health = (int) Math.min(10, (available / 5000.0) * 10);
        String bar = "🟩".repeat(Math.max(0, health)) + "⬜".repeat(Math.max(0, 10 - health));

        String msg = "📊 **СТАТУС:**\n💰 Доступно: " + String.format("%.2f", available) + " сом\n📅 Дней: " + days + "\n📉 Лимит: **" + String.format("%.2f", limit) + "**\n━━━━━━━━━━━━━━\n" + bar;
        sendText(chatId, msg);
    }

    private void sendHelp(long chatId) {
        sendText(chatId, "❓ **ПОМОЩЬ**\n/status - остаток\n/nextday - новый день\n/start - сброс\nПросто пиши цену покупки.");
    }

    private void initDb() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:finguard.db")) {
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY, bal REAL, res REAL, d INTEGER)");
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void saveUserData() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:finguard.db")) {
            PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO users (id, bal, res, d) VALUES (1, ?, ?, ?)");
            ps.setDouble(1, balance);
            ps.setDouble(2, reserve);
            ps.setInt(3, days);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void loadUserData() {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:finguard.db")) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM users WHERE id = 1");
            if (rs.next()) {
                balance = rs.getDouble("bal");
                reserve = rs.getDouble("res");
                days = rs.getInt("d");
                currentState = State.WORK;
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void sendText(Long who, String what) {
        SendMessage sm = SendMessage.builder().chatId(who.toString()).text(what).parseMode("Markdown").build();
        try { execute(sm); } catch (TelegramApiException e) { e.printStackTrace(); }
    }
}