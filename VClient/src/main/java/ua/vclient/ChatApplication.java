package ua.vclient;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ChatApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ChatApplication.class.getResource("/ChatView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 700, 700);
        stage.setTitle("VChat");
        stage.setScene(scene);
        stage.show();
        ChatControl controller = fxmlLoader.getController();
        stage.setOnCloseRequest(event -> controller.close());
    }

    public static void main(String[] args) {
        launch();
    }
}