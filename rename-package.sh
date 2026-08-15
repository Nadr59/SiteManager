#!/bin/bash

OLD_PATH="app/src/main/java/com/example/sitemanager"
NEW_PATH="app/src/main/java/com/nadr59/sitemanager"

echo "=== 1. إنشاء المسار الجديد ==="
mkdir -p "app/src/main/java/com/nadr59"

echo "=== 2. نقل الملفات ==="
mv "$OLD_PATH" "$NEW_PATH"

echo "=== 3. حذف المجلد القديم ==="
rm -rf "app/src/main/java/com/example"

echo "=== 4. تحديث package declarations ==="
find "$NEW_PATH" -name "*.kt" -exec sed -i 's/package com\.example\.sitemanager/package com.nadr59.sitemanager/g' {} +

echo "=== 5. تحديث imports ==="
find "$NEW_PATH" -name "*.kt" -exec sed -i 's/import com\.example\.sitemanager/import com.nadr59.sitemanager/g' {} +

echo "=== 6. تحديث build.gradle.kts ==="
sed -i 's/namespace = "com.example.sitemanager"/namespace = "com.nadr59.sitemanager"/g' app/build.gradle.kts

echo "=== 7. تحديث AndroidManifest (إن وجد) ==="
sed -i 's/com\.example\.sitemanager/com.nadr59.sitemanager/g' app/src/main/AndroidManifest.xml 2>/dev/null

echo "=== 8. التحقق من النتائج ==="
echo ""
echo "الملفات المنقولة:"
find "$NEW_PATH" -type f -name "*.kt" | sort

echo ""
echo "الحزمات في الملفات:"
grep -r "^package " "$NEW_PATH" --include="*.kt" | head -10

echo ""
echo "الاستيرادات القديمة المتبقية (يجب أن يكون صفر):"
grep -r "com.example.sitemanager" "$NEW_PATH" --include="*.kt" | wc -l

echo ""
echo "=== تم بنجاح! ==="
