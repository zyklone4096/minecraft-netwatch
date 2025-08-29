# NetWatch

想不出名字了

Minecraft 全自部署云黑插件

## 构建
- Windows 服务端 `gradle buildServerWinX64` -> `build/NetWatch-windows-amd64.exe`
- Linux 服务端 `gradle buildServerLinuxX64` -> `build/NetWatch-linux-amd64`
- Paper 插件 `gradle :paper:shadowJar` -> `paper/build/libs/NetWatch-paper-*-all.jar`
- Velocity 插件 `gradle :velocity:shadowJar` -> `velocity/build/libs/NetWatch-velocity-*-all.jar`
