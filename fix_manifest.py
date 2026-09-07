import re
with open('app/src/main/AndroidManifest.xml', 'r') as f:
    data = f.read()
data = data.replace('android:theme="@style/Theme.MyApplication"\n            android:windowSoftInputMode="adjustResize">', 'android:theme="@style/Theme.MyApplication">', 1)
with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(data)
