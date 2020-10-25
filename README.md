﻿> *其他语言版本：[简体中文](README.zh.md)*

This page introduces how to run the Android sample project.

## Prerequisites 

- Make sure you have made the preparations mentioned in the [Agora e-Education Guide](https://github.com/AgoraIO-Usecase/eEducation).
- Prepare the development environment:
  - JDK
  - Android Studio 2.0  or later
- Real Android devices, such as Nexus 5X. We recommend using real devices because some function may not work well on simulators or you may encounter performance issues.

## Run the sample project

Follow these steps to run the sample project:

**1.Clone the repository to your local machine.**

```
git clone https://github.com/AgoraIO-Usecase/eEducation.git
```

**2.Enter the directory of the Android project.**

```
cd eEducation/education_Android/
```

**3.Open the Android project with Android Studio.**

**4.Configure keys(Required).**

Pass the following parameters in `app/src/main/res/values/string_configs.xml`:
- The Agora App ID that you get.
- The Agora Customer ID that you get.
- The Agora Customer Certificate that you get.
- The WhiteBoard AppIdentifier that you get.

```
<string name="agora_app_id_dev" translatable="false">Your AppId with devMode</string>
<string name="agora_customer_id_dev" translatable="false">Your customerId with devMode</string>
<string name="agora_customer_cer_dev" translatable="false">Your customerCer with devMode</string>

<string name="agora_app_id_prod" translatable="false">Your AppId with prodMode</string>
<string name="agora_customer_id_prod" translatable="false">Your customerId with devMode</string>
<string name="agora_customer_cer_prod" translatable="false">Your customerCer with devMode</string>
```
Dev is the configuration in the development environment and prod is the configuration in the production environment. If you have only one set of data, then the two environments can be configured as the same set of data directly; at the same time, you can set up the `eEducation/education_Android/edusdk-api/build.gradle` to change the current mode.

```
/**可配置项*/
buildConfigField 'Boolean', 'isDevMode', 'false'
```

For details, see the [prerequisites](https://github.com/AgoraIO-Usecase/eEducation/blob/master/README.md#prerequisites) in Agora E-education Guide.

**5.Run the project.**

## Connect us

- You can read the full set of documentations and API reference at [Agora Developer Portal](https://docs.agora.io/en/).
- You can ask for technical support by submitting tickets in [Agora Console](https://dashboard.agora.io/). 
- You can submit an [issue](https://github.com/AgoraIO-Usecase/eEducation/issues) if you find any bug in the sample project. 

## License

Distributed under the MIT License. See `LICENSE` for more information.