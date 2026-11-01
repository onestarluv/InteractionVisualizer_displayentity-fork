# kokonut_ineteraction

Maintained by **onestarluv**: [fork repository](https://github.com/onestarluv/InteractionVisualizer_displayentity-fork).
Based on [LOOHP/InteractionVisualizer](https://github.com/LOOHP/InteractionVisualizer); original author: LOOHP.

The plugin resource links below refer to the upstream project.

https://www.spigotmc.org/resources/77050/<br>
https://modrinth.com/plugin/interactionvisualizer<br>
https://hangar.papermc.io/LOOHP/InteractionVisualizer

Visualize Function Blocks like Crafting Tables with Animations CLIENT-SIDE!

More information (screenshots, commands, permissions) about the plugin can be found on the Spigot page linked above.

## 한국어 명령어

- `/시각효과 토글 전체 끄기` — 모든 개인 시각 효과 끄기
- `/시각효과 토글 전체 켜기` — 모든 개인 시각 효과 켜기
- `/시각효과 토글 벌통 끄기` — 벌통의 모든 효과 끄기
- `/시각효과 새로고침` — 본인 화면의 시각 효과 다시 표시
- `/시각효과 리로드` — 설정 다시 불러오기 (관리자)
- `/시각효과 업데이트` — 원본 프로젝트 버전 확인

아이템·드롭·홀로그램 종류를 나누지 않고 한 번에 설정합니다. 켜기/끄기를 생략하면 해당 항목의 효과가 하나라도 켜져 있을 때 모두 끄고, 모두 꺼져 있을 때 모두 켭니다. 다른 플레이어를 지정하려면 관리자 권한이 필요합니다. 한국어 자동완성과 블록 이름을 지원합니다. `/iv`와 기존 영어 인자는 호환용으로 사용할 수 있습니다.

기존 설치에서 이름을 변경할 때는 서버를 종료한 뒤 plugins/InteractionVisualizer의 데이터를 plugins/kokonut_ineteraction으로 복사해야 개인 설정 DB와 기존 설정이 이어집니다.

## 한국어 명칭 기준

블록 이름은 [Mojang 공식 26.3 한국어 언어 파일](https://resources.download.minecraft.net/10/1024e120d7c35438d9d1f5bfee3906b29a867c1c)을 기준으로 표시합니다.

| 기존 표기 | 수정한 표기 |
| --- | --- |
| 인챈트대 | 마법 부여대 |
| 도관 | 전달체 |
| 생성기 | 몬스터 생성기 |
| 지도제작대 | 지도 제작대 |
| 대장장이작업대 | 대장장이 작업대 |
| 엔더상자 / 셜커상자 | 엔더 상자 / 셜커 상자 |

`/시각효과 토글 마법 부여대 끄기`처럼 띄어쓴 공식 이름과 붙여쓴 이름을 모두 입력할 수 있습니다. 기존 `인챈트대`, `도관`, `생성기` 별칭도 인식합니다. 자동완성과 기본 안내에는 공식 명칭을 사용합니다.

## 데이터베이스 저장 경로

SQLite 데이터베이스는 현재 플러그인 데이터 폴더인 `plugins/kokonut_ineteraction/database.db`에 저장됩니다. 신규 설치에서는 폴더와 DB를 자동으로 생성하며, 이전 이름의 `InteractionVisualizer` 폴더를 따로 만들 필요가 없습니다.

DB 초기화에 실패하면 플러그인 시작을 중단합니다. 종료 시에는 초기화된 매니저만 정리하므로 초기화 실패에 따른 추가 null 오류를 방지합니다.

## 서버 공통 설정

주 설정은 `ServerSetting` enum에서 관리하며, `ServerSettings` record가 서버 전체에서 공유하는 읽기 전용 설정을 제공합니다. enum 값을 수정한 뒤 빌드하면 새 기본값이 적용됩니다. 신규 서버에는 주 `config.yml`을 생성하지 않습니다.

기존 서버의 `config.yml`이 있으면 사용자 값을 보존해 공통 record에 가져옵니다. 기존 파일을 삭제하거나 덮어쓰지 않으며, 이 호환 파일의 값은 코드 기본값보다 우선합니다. 개인별 `/시각효과 토글` 설정은 계속 UUID별 DB에 저장됩니다. 재료 분류와 음악 데이터 파일은 기존 형식을 유지합니다.

## Supported version

This fork supports Minecraft **26.3 only**. Older version implementations are removed.

## Building this fork

Requires JDK 25. Open the root `settings.gradle.kts` in IntelliJ as a Gradle project.

```powershell
.\gradlew.bat clean build
```

On Linux/macOS use `./gradlew clean build`. The deployable plugin is
`build/libs/kokonut_ineteraction-2026.1.4.0.jar` (`-plain.jar` excludes bundled libraries).

This fork targets Paper 26.3. The Gradle `paperweight-userdev` plugin downloads
and prepares Paper's development bundle, including API and server internals.
No local CraftBukkit installation or BuildTools run is required.
The existing source directories are compiled together as one Gradle project.

## Display rendering

The existing armor stand holders remain as animation state. Their hand and head
items are rendered using packet-only ItemDisplay entities, and visible names use
TextDisplay entities. Empty parts are not spawned on clients. Viewer filtering,
viewer-specific hologram positions, and removal apply to every display part.
No display entities are registered in the server world.

Text faces each viewer with vertical rotation only, uses maximum brightness,
and remains visible through blocks so furnace and hive labels stay readable
from the side. Existing distance and player visibility filters still apply.

Dropped-item physics and item-frame map rendering retain their existing packet
paths to preserve those features. Item pose offsets should be checked in-game,
including custom resource-pack models.

## Upstream Development Builds

- [Jenkins](https://ci.loohpjames.com/job/InteractionVisualizer/)

## Upstream Maven dependency
```html
<repository>
  <id>loohp-repo</id>
  <url>https://repo.loohpjames.com/repository</url>
</repository>
```
```html
<dependency>
  <groupId>com.loohp</groupId>
  <artifactId>InteractionVisualizer</artifactId>
  <version>VERSION</version>
  <scope>provided</scope>
</dependency>
```
Replace `VERSION` with the version number.

## Dependencies 

- [ProtocolLib](https://www.spigotmc.org/resources/protocollib.1997/)
- [LightsAPI Fork](https://www.spigotmc.org/resources/lightapi-fork.48247/)

## Soft Dependencies

- [EssentialsX](https://www.spigotmc.org/resources/essentialsx.9089/)
- [OpenInv](https://dev.bukkit.org/projects/openinv)
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)
- [SuperVanish](https://www.spigotmc.org/resources/supervanish-be-invisible.1331/)
- [PremiumVanish](https://www.spigotmc.org/resources/premiumvanish-stay-hidden-bungee-support.14404/)
- [CMI](https://www.spigotmc.org/resources/cmi-270-commands-insane-kits-portals-essentials-economy-mysql-sqlite-much-more.3742/) ([Public API](https://github.com/Zrips/CMI-API))

## Community Maintained Languages
Link to repository: https://github.com/LOOHP/InteractionVisualizerLanguages

## Upstream Partnerships (LOOHP)

### Server Hosting
**Use the link or click the banner** below to **get a 25% discount off** your first month when buying any of their gaming servers!<br>
It also **supports my development**, take it as an alternative way to donate while getting your very own Minecraft server as well!

*P.S. Using the link or clicking the banner rather than the code supports me more! (Costs you no extra!)*

**https://www.bisecthosting.com/loohp**

[![](https://www.bisecthosting.com/partners/custom-banners/96e11ee5-50e4-494f-854d-8c1708813abd.png)](https://www.bisecthosting.com/loohp)
