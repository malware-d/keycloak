# Custom Provider trên KEYCLOAK
- ***Tình huống:*** Ứng dụng DỊCH VỤ CÔNG của thành phố cho phép người dân truy cập vào đó, đăng ký tài khoản của họ và thực hiện những yêu cầu trực tiếp trên ứng dụng. Tuy nhiên, để bảo mật ứng dụng web, người dùng trên nền tảng (bao gồm cả admin và người dùng bình thường) cần phải được ủy quyền đến những resource một cách chính xác.
> Ví dụ: /admin chỉ có thể được truy cập thành công bởi những tài khoản được có role là "quan tri vien"

Trong một hệ thống mạng có quy mô lớn, việc chủ động cấp tài khoản cho những người quản trị viên khác nhau là đơn giản vì số lượng nhỏ, còn đối với người dùng bình thường - số lượng quá lớn, việc cập nhật roles cho họ sau khi họ đã đăng ký tài khoản là việc làm có mức độ hiệu quả bằng 0. Hay đồng nghĩa với việc: ***ngay tại thời điểm người dùng đăng ký tài khoản, tài khoản đó của họ cần phải được gán role phù hợp ngay lập tức***

- ***Giải quyết:*** 
1. Tạo roles phù hợp trong Client (admin DVC role, Nguoi dan DVC role)
2. Tạo Group phù hợp trong Realm (Nguoi dan group, Quan tri vien group)
3. Assign role cần thiết cho Group cụ thể thông qua Role mapping (assign 'Nguoi dan DVC role' cho 'Nguoi dan group' 'Quan tri vien group'; assign 'admin DVC role' cho 'Quan tri vien group')
4. Sử dụng tính năng Event Listener SPI của Keycloak để thực hiện việc tự động thêm người vào group. Trường hợp cụ thể ở đây là: New user sẽ được tự động add vào 'Nguoi dan group'
> Lớp java trong Custom Event Listener SPI sẽ lắng nghe các sự kiện như: REGISTER hoặc LOGIN và sau đó sẽ sử lý các sự kiện này
## Custom Event Listener SPI
1. Tạo 1 dự án Maven cho Event Listener
2. Thêm sự kiện lắng nghe để thêm người dùng vào nhóm
3. Triển khai và cấu hình plugin trong Keycloak
### Bước 1: Tạo dự án Maven
Cấu trúc thư mục tổng thể của dự án
```lua
custom-event-listener/
|-- pom.xml
|-- src/
    |-- main/
        |-- java/
            |-- com/
                |-- example/
                    |-- keycloak/
                        |-- CustomEventListenerProvider.java
                        |-- CustomEventListenerProviderFactory.java
        |-- resources/
            |-- META-INF/
                |-- services/
                    |-- org.keycloak.events.EventListenerProviderFactory

```
1. Tạo cấu trúc sự án Maven
```console
mvn archetype:generate -DgroupId=com.example.keycloak -DartifactId=custom-event-listener -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
```
2. Chuyển vào thư mục dự án
```console
cd custom-event-listener
```
3. Nội dung file 'pom.xml'
```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example.keycloak</groupId>
    <artifactId>custom-event-listener</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <dependencies>
        <dependency>
            <groupId>org.keycloak</groupId>
            <artifactId>keycloak-core</artifactId>
            <version>25.0.0</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.keycloak</groupId>
            <artifactId>keycloak-server-spi</artifactId>
            <version>25.0.0</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.keycloak</groupId>
            <artifactId>keycloak-server-spi-private</artifactId>
            <version>25.0.0</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.keycloak</groupId>
            <artifactId>keycloak-services</artifactId>
            <version>25.0.0</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.jboss.logging</groupId>
            <artifactId>jboss-logging</artifactId>
            <version>3.4.1.Final</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>

```
4. Tạo file 'CustomEventListenerProvider.java' trong 'src/main/java/com/example/keycloak/'
```java
package com.example.keycloak;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.GroupModel;

//Khai báo lớp 'CustomEventListenerProvider' và nó sẽ triển khai (implements) interface 'EventListenerProvider'
public class CustomEventListenerProvider implements EventListenerProvider {

    private final KeycloakSession session;

    public CustomEventListenerProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void onEvent(Event event) {
        if (EventType.REGISTER.equals(event.getType())) {
            RealmModel realm = session.realms().getRealm(event.getRealmId()); //Lấy đối tượng realm dựa trên ID của realm từ sự kiện
            UserModel user = session.users().getUserById(realm, event.getUserId()); //Lấy đối tượng người dùng dựa trên ID của người dùng từ sự kiện
            GroupModel group = session.groups().getGroupByName(realm, null, "Nguoi dan"); //Lấy đối tượng nhóm "Nguoi dan" từ realm

            if (group != null && user != null) {
                user.joinGroup(group);
            }
        }
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // Không xử lý sự kiện AdminEvent, nhưng phương thức này cần có mặt
    }

    @Override
    public void close() {
    }
}
```
5. Tạo file 'CustomEventListenerProviderFactory.java' trong 'src/main/java/com/example/keycloak/'
```java
package com.example.keycloak;

import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class CustomEventListenerProviderFactory implements EventListenerProviderFactory {

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new CustomEventListenerProvider(session);
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
        // Khởi tạo nếu cần
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // Khởi tạo sau nếu cần
    }

    @Override
    public void close() {
        // Đóng nếu cần
    }

    @Override
    public String getId() {
        return "custom-event-listener";
    }
}
```
> **CustomEventListenerProvider:** Lớp này chịu trách nhiệm xử lý sự kiện khi một người dùng đăng ký tài khoản
**CustomEventListenerProviderFactory:** Lớp này tạo ra một instance của CustomEventListenerProvider
6. (Tạo tệp cấu hình SPI) - Tạo thư mục 'META-INF/services' trong 'src/main/resources/'
```sh
mkdir -p src/main/resources/META-INF/services
```
7. Tạo tệp 'org.keycloak.events.EventListenerProviderFactory' trong 'src/main/resources/META-INF/services/'
```sh
touch src/main/resources/META-INF/services/org.keycloak.events.EventListenerProviderFactory
```
8. Thêm nội dung vào tệp 'org.keycloak.events.EventListenerProviderFactory'
```sh
com.example.keycloak.CustomEventListenerProviderFactory
```
### Bước 2: Build và Deploy
1. Build dự án bằng Maven
```sh
mvn clean package
```
Xóa tệp 'AppTest.java' trong thư mục 'src/test/java/com/example/keycloak' - đây là file được tự động tạo khi tạo project bằng maven, không liên quan đến plugin
```sh
rm src/test/java/com/example/keycloak/AppTest.java
``` 

2. Deploy tệp JAR vào Keycloak
Sao chép tệp JAR (target/custom-event-listener-1.0-SNAPSHOT.jar) vào thư mục providers của Keycloak
3. Khởi động lại Keycloak
```sh
./kc.sh build
/kc.sh start-dev --spi-login-protocol-openid-connect-legacy-logout-redirect-uri=true 
./kc.sh start-dev --spi-events-listener=custom-event-listener //nếu gặp lỗi thì mới thử lệnh này
```

### Hướng dẫn cài đặt từ keycloak
```md
Installing Custom Providers
===========================

Add your custom provider JAR files in this directory.

Once you have your providers in this directory, run the following command to complete the installation:


${kc.home.dir}/bin/kc.sh build
```
## META-INF trong keycloak là gì?
Trong Keycloak, thư mục 'META-INF' là một phần quan trọng của cấu trúc dự án Java. Nó chứa các metadata cần thiết để Keycloak có thể nhận diện và cấu hình các thành phần của bạn, chẳng hạn như các providers tùy chỉnh. 

Dưới đây là một số thông tin cụ thể về vai trò của thư mục META-INF:
1. 'META-INF' trong các thư viện Java: Thư mục 'META-INF' thường chứa các tệp như 'MANIFEST.MF' và các tệp cấu hình khác mà Java cần để biết cách xử lý các nội dung trong tệp JAR. Tệp MANIFEST.MF chứa các thông tin như tên của package, phiên bản, và các thuộc tính khác
2. Keycloak Providers
- Khi bạn triển khai một provider tùy chỉnh trong Keycloak, bạn cần đặt một tệp cấu hình trong META-INF/services. Tệp này sẽ chỉ ra rằng provider của bạn là một implementation của một trong các SPI (Service Provider Interfaces) của Keycloak

> Ví dụ: Nếu bạn đang tạo một Event Listener Provider, bạn sẽ cần tạo một tệp trong 'META-INF/services' với tên 'org.keycloak.events.EventListenerProviderFactory' và nội dung của tệp này sẽ là tên đầy đủ của lớp factory cho provider của bạn
3. Cấu trúc thư mục
- 'META-INF/MANIFEST.MF': Thông tin chung về tệp JAR
- 'META-INF/services/': Chứa các tệp định danh các provider cụ thể