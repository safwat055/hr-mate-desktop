#!/bin/bash
# count_lines.sh - يحسب عدد الأسطر في جميع الملفات النصية فقط (يستثني الصور والخطوط)

echo "📁 جاري حساب عدد الأسطر في جميع الملفات النصية..."
echo "============================================="

# استثناء الامتدادات التالية (صور، خطوط، ملفات ثنائية، إلخ)
exclude_exts="*.png|*.jpg|*.jpeg|*.gif|*.ico|*.bmp|*.tiff|*.svg|*.ttf|*.otf|*.woff|*.woff2|*.eot|*.pdf|*.jar|*.zip|*.gz|*.tar|*.xz"

# تعريف مصفوفة لتخزين النتائج
declare -a files=()
declare -a lines=()

# البحث عن الملفات (مع استثناء الامتدادات المحددة)
while IFS= read -r -d '' file; do
    # تحقق من الامتداد
    ext="${file##*.}"
    if [[ "$ext" =~ ^(png|jpg|jpeg|gif|ico|bmp|tiff|svg|ttf|otf|woff|woff2|eot|pdf|jar|zip|gz|tar|xz)$ ]]; then
        continue
    fi
    # تجاهل الملفات الثنائية (باستخدام أمر file)
    if file "$file" | grep -q "binary"; then
        continue
    fi
    # حساب الأسطر
    lines_count=$(wc -l < "$file" 2>/dev/null)
    if [ $? -eq 0 ]; then
        files+=("$file")
        lines+=("$lines_count")
    fi
done < <(find . -type f -print0)

# عرض النتائج
total=0
for i in "${!files[@]}"; do
    printf "%-70s %8d\n" "${files[$i]}" "${lines[$i]}"
    total=$((total + lines[i]))
done

echo "============================================="
echo "✅ إجمالي الأسطر في جميع الملفات النصية: $total"