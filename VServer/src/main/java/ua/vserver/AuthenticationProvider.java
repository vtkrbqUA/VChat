package ua.vserver;

import java.util.Optional;

public interface AuthenticationProvider {
    Optional<String> getNicknameByLoginAndPassword(String login, String password);

    Boolean isLoginBusy(String login);

    Boolean isNicknameBusy(String nickname);

    void addUserToDB(String login, String password, String nickname);

    void removeUserFromDB(String login);

    void changeNickname(String oldNickname, String newNickname);

    void shutdown();
}
