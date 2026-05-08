@echo off
chcp 65001 >nul
cd /d D:\yethingchan.github.io

:: 输入提交信息
set /p "COMMIT_MSG=Enter commit message (Press Enter directly to push only): "

:: 判断是否直接回车（无提交信息）
if "%COMMIT_MSG%"=="" (
    echo ==============================================
    echo Pushing local unpushed commits...
    git push origin main
    echo Push operation finished!
) else (
    :: 正常流程：提交+推送
    git add .
    git commit -m "%COMMIT_MSG%"
    git push origin main
    echo Commit and push completed!
)

pause