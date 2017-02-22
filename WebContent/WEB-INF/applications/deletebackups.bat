rem find all directories called _backups and delete them and their contents
FOR /d /r . %%d IN (_backups) DO @IF EXIST "%%d" rd /s /q "%%d"