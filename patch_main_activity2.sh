#!/bin/bash
sed -i 's/setContent {/setContent {\n            if (crashTrace != null) {\n                com.example.ui.screens.CrashScreen(crashTrace!!) {\n                    crashTrace = null\n                }\n                return@setContent\n            }/g' app/src/main/java/com/example/MainActivity.kt
