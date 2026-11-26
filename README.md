# Call-App

An Android dialer written in Kotlin that can replace the phone's default calling app. It places and receives GSM calls through Android's Telecom framework, with its own in-call screens, contacts and call history.

## What it does

- **Default dialer role**: handles `DIAL` and `tel:` intents, so the system routes calls to it.
- **In-call service**: `GSMCallService` plugs into the Telecom framework; `CallManager` tracks call state (incoming, outgoing, active, held).
- **Incoming calls**: heads-up and full-screen notifications with answer and decline actions.
- **In-call controls**: keypad, mute, speaker, hold.
- **Contacts and call log**: reads the device contacts (with avatars) and call history through content providers.
- **Dual SIM**: lists the phone accounts so you can choose which SIM places the call.

## Stack

Kotlin 1.7 · AndroidX (Navigation, ViewBinding, Lifecycle) · Material Components · Glide · Gson
Min SDK 28 (Android 9) · Target SDK 32

## Project layout

```
app/src/main/java/com/kapp/call_app/
├── telecom/          # InCallService + call state management
├── external_call/    # dialer and in-call activities
├── notifications/    # incoming-call notifications and their actions
├── contacts/         # contacts lookup and avatars
├── call_logs/        # call history
└── models/           # Contact, PhoneNumber, SIMAccount…
```

## Run it

Open the project in Android Studio and run it on a physical device with a SIM card (emulators cannot place real calls). When the app asks, set it as the default phone app so it can receive calls.

## Status

A personal project from 2022, kept here as a reference for working with the Android Telecom APIs. It is not maintained.
