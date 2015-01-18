@echo off
pushd %~dp0

rem ===== �N���[�� =====
if exist release rmdir /s /q release
mkdir release

rem ===== BukkitDev���������[�X�t�@�C���̍쐬 =====
move /y pom.xml pom.xml.backup
java -jar XmlSetter.jar pom.xml.backup pom.xml release.lang en
call mvn clean deploy
pushd target
ren Undine-*-dist.zip Undine-*-en.zip
popd
move /y target\Undine-*-en.zip release\

rem ===== ���{�t�H�[�������������[�X�t�@�C���̍쐬 =====
java -jar XmlSetter.jar pom.xml.backup pom.xml release.lang ja
call mvn clean javadoc:jar source:jar deploy
pushd target
ren Undine-*-dist.zip Undine-*-ja.zip
popd
move /y target\Undine-*-ja.zip release\

rem ===== ��Еt�� =====
move /y pom.xml.backup pom.xml

popd
