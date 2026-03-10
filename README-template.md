<div align="center"> 
<img src="assets/rvx-logo.png" alt="RVX logo" width="128">

    
## 🧩 ReVanced Extended Patches

ReVanced Extended Patches. 
    
[![Static Badge](https://img.shields.io/badge/RVX_Documentation-gray?style=flat-square&logo=github)](https://github.com/inotia00/revanced-documentation#readme)   [![Static Badge](https://img.shields.io/badge/Reddit-gray?style=flat-square&logo=reddit)](https://reddit.com/r/revancedextended)   [![Static Badge](https://img.shields.io/badge/Discord-gray?style=flat-square&logo=discord)](https://discord.gg/yMnc3EywRZ)
<br>
[![Static Badge](https://img.shields.io/badge/Telegram-Announcements-gray?style=flat-square&logo=telegram&color=%2326A5E4)](https://t.me/revanced_extended)   [![Static Badge](https://img.shields.io/badge/Telegram-Chat-gray?style=flat-square&logo=telegram&color=%2326A5E4)](https://t.me/revanced_extended_chat)   [![Static Badge](https://img.shields.io/badge/Telegram-GitHub_Notifications-gray?style=flat-square&logo=telegram&color=%2326A5E4)](https://t.me/revanced_extended_repo)
<br>
[![Static Badge](https://img.shields.io/badge/Translations-YouTube-gray?style=flat-square&logo=crowdin&color=%23f5f5f5)](https://crowdin.com/project/revancedextended)   [![Static Badge](https://img.shields.io/badge/Translations-YT_Music-gray?style=flat-square&logo=crowdin&color=%23f5f5f5)](https://crowdin.com/project/revancedmusicextended)
<br>
</div> 

See the [documentation](https://github.com/inotia00/revanced-documentation#readme) to learn how to apply patches and build ReVanced Extended apps.

~~Report issues [here](https://github.com/inotia00/ReVanced_Extended).~~

## ⚠️ DEPRECATED

This project is **deprecated** and no longer actively maintained.

- No further features, fixes, or updates will be provided.
- Issues and pull requests are no longer monitored.
- The repository remains available for reference and forking.

**See [Announcement](https://github.com/inotia00/ReVanced_Extended/issues/3334) for more info**.

## 🔀 Recommended Alternatives

- **[Morphe](https://github.com/MorpheApp) (Recommended)**: This project is an alternative to ReVanced, which no longer has an active maintainer. Considering its future sustainability, I recommend it as a long-term successor project. I will continue to contribute as a member of the Morphe Team.
- [RVX by Anddea](https://github.com/anddea/revanced-patches): This is a fork of RVX that has been maintained for about 2 years.

For immediate compatibility, [RVX by Anddea](https://github.com/anddea/revanced-patches) is available; however, for sustainable support, I recommend migrating to **[Morphe](https://github.com/MorpheApp)**.

## 🤝 A Final Note of Thanks

It has been an incredible journey over the past 3.5 years. I am deeply grateful to the community for all the support and contributions that made this project possible.

While this repository is now archived, its spirit continues in [Morphe](https://github.com/MorpheApp). Thank you for being part of the ride!

## 📋 List of patches in this repository

{{ table }}

## 📝 JSON Format

This section explains the JSON format for the [patches.json](patches.json) file.

Example:

```json
[
  {
    "name": "Alternative thumbnails",
    "description": "Adds options to replace video thumbnails using the DeArrow API or image captures from the video.",
    "use":true,
    "compatiblePackages": {
      "com.google.android.youtube": "COMPATIBLE_PACKAGE_YOUTUBE"
    },
    "options": []
  },
  {
    "name": "Bitrate default value",
    "description": "Sets the audio quality to 'Always High' when you first install the app.",
    "use":true,
    "compatiblePackages": {
      "com.google.android.apps.youtube.music": "COMPATIBLE_PACKAGE_MUSIC"
    },
    "options": []
  },
  {
    "name": "Hide ads",
    "description": "Adds options to hide ads.",
    "use":true,
    "compatiblePackages": {
      "com.reddit.frontpage": "COMPATIBLE_PACKAGE_REDDIT"
    },
    "options": []
  }
]
```
