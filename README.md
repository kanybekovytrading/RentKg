# 🏠 Аренда Бишкек — Telegram Bot

## Стек
- Java 17
- Spring Boot 3.2
- PostgreSQL 16
- Flyway (миграции)
- Docker Compose

## Быстрый старт

```bash
# 1. Скопируй переменные окружения
cp .env.example .env
# Заполни .env своими данными

# 2. Запусти
docker-compose --env-file .env up -d
```

## Структура проекта

```
src/main/java/kg/rental/
├── RentalBotApplication.java
├── bot/
│   ├── RentalBot.java              # Telegram Long Polling бот
│   ├── Keyboards.java              # Все клавиатуры
│   └── handler/
│       ├── MessageHandler.java     # Обработка сообщений (вся логика диалога)
│       └── CallbackHandler.java    # Обработка inline кнопок
├── config/
│   ├── AppConfig.java
│   └── TelegramConfig.java
├── entity/                         # JPA сущности
├── enums/                          # Перечисления
├── repository/                     # Spring Data репозитории
├── service/
│   ├── UserService.java
│   ├── ListingService.java
│   ├── ComplaintService.java
│   ├── NotificationService.java
│   ├── TelegramChannelService.java
│   └── MessageHelper.java
└── scheduler/
    └── ListingScheduler.java       # Напоминания и архивация
```

## Логика статусов

```
🟢 ACTIVE   → напоминание через 3 дня → 🟡 PENDING
🟡 PENDING  → хозяин подтвердил → 🟢 ACTIVE
🟡 PENDING  → 7 дней без ответа → 📦 ARCHIVED
✅ CLOSED   → хозяин закрыл вручную
🚫 При 3 жалобах SCAMMER → ARCHIVED + в чёрный список
```

## Переменные окружения

| Переменная | Описание |
|---|---|
| TELEGRAM_BOT_TOKEN | Токен от @BotFather |
| TELEGRAM_BOT_USERNAME | Username бота |
| CHANNEL_MAIN | Главный канал |
| CHANNEL_RENT_OUT | Канал «Сдаю квартиру» |
| CHANNEL_RENT_IN | Канал «Ищу квартиру» |
| CHANNEL_ROOMMATE | Канал подселения |
| CHANNEL_BLACKLIST | Канал чёрного списка |

## Важно перед запуском

1. Создать все 5 каналов в Telegram
2. Добавить бота как **администратора** в каждый канал
3. Заполнить `.env`
