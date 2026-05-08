@echo off
chcp 65001 >nul
cd /d D:\yethingchan.github.io
set /p "COMMIT_MSG=Enter commit message: "
git add .
git commit -m "%COMMIT_MSG%"
git push -u origin main
echo Commit completed!
pause