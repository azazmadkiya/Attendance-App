import os
import shutil

old_path = "app/src/main/java/com/example"
new_path = "app/src/main/java/com/attendance/app/azaz"

print(f"Creating new directory structure: {new_path}")
os.makedirs(new_path, exist_ok=True)

# 1. Move files recursively from old_path to new_path
if os.path.exists(old_path):
    for root, dirs, files in os.walk(old_path, topdown=False):
        rel_path = os.path.relpath(root, old_path)
        target_dir = os.path.join(new_path, rel_path) if rel_path != "." else new_path
        os.makedirs(target_dir, exist_ok=True)
        for file in files:
            src_file = os.path.join(root, file)
            dst_file = os.path.join(target_dir, file)
            shutil.move(src_file, dst_file)
            print(f"Moved {src_file} -> {dst_file}")

    shutil.rmtree(old_path, ignore_errors=True)

# 2. Update package declarations and imports in all .kt and .xml files in app/src/main
print("Updating package names and imports in source files...")
for root, dirs, files in os.walk("app/src/main"):
    for file in files:
        if file.endswith((".kt", ".xml", ".kts")):
            file_path = os.path.join(root, file)
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read()
            
            new_content = content.replace("com.example", "com.attendance.app.azaz")
            if new_content != content:
                with open(file_path, "w", encoding="utf-8") as f:
                    f.write(new_content)
                print(f"Updated references in {file_path}")

print("Refactoring completed successfully!")
