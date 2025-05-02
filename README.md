# ktor-cronet

This is an implementation of a Ktor client engine, which uses
[Cronet](https://developer.android.com/develop/connectivity/cronet/start)

## Usage

```kotlin
dependencies {
    implementation("com.trainyapp.cronet", "ktor-cronet", "1.0.0")
}
```

```kotlin
val httpClient = HttpClient(Cronet) {

}
```

## Fallback

Cronet is only available on Google Play certified devices, if you want to support other devices add
[this](https://mvnrepository.com/artifact/org.chromium.net/cronet-fallback) dependency
