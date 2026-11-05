# ARKITAK Lite

হালকা ভয়েস অ্যাসিস্ট্যান্ট: পুরো স্ক্রিন জুড়ে 3D orb, আর Gemini Live দিয়ে সরাসরি কথোপকথন।

## নতুন (এই আপডেটে)
- **Full-screen আলো ও ডট:** orb-এর আলো ও ছোট হালকা ডটগুলো এখন পুরো স্ক্রিনে ছড়ানো। কথা বললে বা Gemini বললে আলো স্ক্রিন জুড়ে নড়ে। Pinch করলে শুধু orb ছোট-বড় হয়।
- **Gemini Live কথোপকথন** (মূল ARKITAK অ্যাপ থেকে আনা): মাইকের অডিও সরাসরি Gemini-তে যায়, Gemini-র গলা সরাসরি আসে। "শোনো → ভাবো → বলো" ধাপ নেই। Gemini নিজেই বোঝে আপনি কখন থামলেন, আর কথার মাঝে থামালে সে থেমে যায়। Gemini নিজের গলা নিজে শুনে ফেলে না (echo guard)।
- **Mic always on:** চালু থাকলে অ্যাপ বন্ধ বা স্ক্রিন অফ থাকলেও মাইক ও কথোপকথন চলে (Android-এর জন্য ছোট "ARKITAK is listening" নোটিফিকেশন থাকে, সেখানে Stop আছে)।
- **Gemini settings:** orb-এ লং-প্রেস → "Gemini settings"। API key, Live model, voice, ভাষা, Mic always on, আর **Test connection** (key/model ঠিক আছে কিনা সরাসরি বলে দেয়)।

## কীভাবে চালাবেন
1. GitHub Actions থেকে debug APK বানান (`.github/workflows/build-debug-apk.yml`)।
2. অ্যাপ খুলুন, API key দিন (aistudio.google.com/apikey), মাইক পারমিশন দিন।
3. কথা বলুন। Orb-এ ট্যাপ করলে Gemini-র কথা থামে। লং-প্রেস করলে মেনু।

## সীমাবদ্ধতা
- Live session মাঝে মাঝে Google নিজে শেষ করে; অ্যাপ নিজে আবার সংযোগ করে, কিন্তু আগের কথার প্রসঙ্গ মনে থাকে না।
- Live model-এর নাম মূল ARKITAK অ্যাপ থেকে নেওয়া। অন্য নাম Gemini settings-এ বসানো যায়।
- আগের SpeechRecognizer + TTS পদ্ধতি সরানো হয়েছে (`_backup_original/`-এ আছে)।
