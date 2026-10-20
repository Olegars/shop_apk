# 0451 Store (магазинное Android-приложение)

WebView-обёртка портала магазина `https://0451.space/store`. Клубная админка и сайт для гостей в приложении недоступны.

- Пакет: `space.club0451.store`
- Имя на устройстве: **0451 Store**
- User-Agent: `CompClubStore/…`
- Старт: `/store/login`
- Скачать: личный кабинет `/admin/salary` у сотрудника магазина, файл `GET /store-app.apk`

Сборка (JDK 17):

```
cd C:\Qt\shop_apk
set JAVA_HOME=C:\Qt\jdk-17
gradlew.bat assembleRelease
```

APK: `app\build\outputs\apk\release\app-release.apk`

После выкладки скопировать в `compclubbooking/storage/app/apk/store0451.apk` и поднять `STORE_APP_VERSION_CODE` / `STORE_APP_VERSION_NAME` в `.env`.
