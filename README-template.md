<div align="center"> 

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

Report issues [here](https://github.com/inotia00/ReVanced_Extended).

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
