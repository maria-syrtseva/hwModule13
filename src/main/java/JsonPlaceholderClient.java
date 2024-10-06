import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class JsonPlaceholderClient {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

    public static void main(String[] args) throws IOException {

        User newUser = new User(11, "New User", "newuser@example.com");
        User createdUser = createUser(newUser);
        System.out.println("Created User: " + createdUser);

        createdUser.setName("Updated User");
        User updatedUser = updateUser(createdUser);
        System.out.println("Updated User: " + updatedUser);

        boolean deleted = deleteUser(createdUser.getId());
        System.out.println("User Deleted: " + deleted);

        List<User> users = getAllUsers();
        System.out.println("All Users: " + users);

        User userById = getUserById(1);
        System.out.println("User by ID: " + userById);

        List<User> userByUsername = getUserByUsername("Bret");
        System.out.println("User by Username: " + userByUsername);

        int userId = 1;
        List<Comment> comments = getCommentsForLastPost(userId);
        saveCommentsToFile(userId, comments);

        List<Todo> openTodos = getOpenTodos(userId);
        System.out.println("Open Todos for User " + userId + ": " + openTodos);
    }

    private static User createUser(User user) throws IOException {
        String endpoint = BASE_URL + "/users";
        String jsonInputString = user.toJson();

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        String jsonResponse = readResponse(connection);
        connection.disconnect();

        if (responseCode == HttpURLConnection.HTTP_CREATED) {
            return User.fromJson(jsonResponse);
        }
        return null;
    }

    private static User updateUser(User user) throws IOException {
        String endpoint = BASE_URL + "/users/" + user.getId();
        String jsonInputString = user.toJson();

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        String jsonResponse = readResponse(connection);
        connection.disconnect();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            return User.fromJson(jsonResponse);
        }
        return null;
    }

    private static boolean deleteUser(int id) throws IOException {
        String endpoint = BASE_URL + "/users/" + id;

        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("DELETE");

        int responseCode = connection.getResponseCode();
        connection.disconnect();
        return (responseCode >= 200 && responseCode < 300);
    }

    private static List<User> getAllUsers() throws IOException {
        String endpoint = BASE_URL + "/users";
        String jsonResponse = sendRequest(endpoint);
        return User.fromJsonArray(jsonResponse);
    }

    private static User getUserById(int id) throws IOException {
        String endpoint = BASE_URL + "/users/" + id;
        String jsonResponse = sendRequest(endpoint);
        return User.fromJson(jsonResponse);
    }

    private static List<User> getUserByUsername(String username) throws IOException {
        String endpoint = BASE_URL + "/users?username=" + username;
        String jsonResponse = sendRequest(endpoint);
        return User.fromJsonArray(jsonResponse);
    }

    private static List<Comment> getCommentsForLastPost(int userId) throws IOException {
        String postsEndpoint = BASE_URL + "/users/" + userId + "/posts";
        String postsResponse = sendRequest(postsEndpoint);
        List<Post> posts = Post.fromJsonArray(postsResponse);

        if (posts.isEmpty()) {
            return new ArrayList<>();
        }

        Post lastPost = posts.stream().max((p1, p2) -> Integer.compare(p1.getId(), p2.getId())).orElse(posts.get(0));

        String commentsEndpoint = BASE_URL + "/posts/" + lastPost.getId() + "/comments";
        String commentsResponse = sendRequest(commentsEndpoint);
        return Comment.fromJsonArray(commentsResponse);
    }

    private static void saveCommentsToFile(int userId, List<Comment> comments) throws IOException {
        if (comments.isEmpty()) {
            System.out.println("No comments to save.");
            return;
        }

        Post lastPost = comments.get(0).getPost();
        String filename = String.format("user-%d-post-%d-comments.json", userId, lastPost.getId());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(Comment.toJsonArray(comments));
            System.out.println("Comments saved to file: " + filename);
        }
    }

    private static List<Todo> getOpenTodos(int userId) throws IOException {
        String endpoint = BASE_URL + "/users/" + userId + "/todos";
        String jsonResponse = sendRequest(endpoint);
        return Todo.fromJsonArray(jsonResponse);
    }

    private static String sendRequest(String endpoint) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        String jsonResponse = readResponse(connection);
        connection.disconnect();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            return jsonResponse;
        }
        return null;
    }

    private static String readResponse(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        return response.toString();
    }

    static class User {
        private int id;
        private String name;
        private String email;

        public User(int id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String toJson() {
            return String.format("{\"id\": %d, \"name\": \"%s\", \"email\": \"%s\"}", id, name, email);
        }

        public static User fromJson(String json) {
            String[] parts = json.replaceAll("[{}\"]", "").split(",");
            int id = Integer.parseInt(parts[0].split(":")[1].trim());
            String name = parts[1].split(":")[1].trim();
            String email = parts[2].split(":")[1].trim();
            return new User(id, name, email);
        }

        public static List<User> fromJsonArray(String json) {
            List<User> users = new ArrayList<>();
            String[] userJsons = json.replaceAll("\\[|\\]", "").split("},");
            for (String userJson : userJsons) {
                users.add(fromJson(userJson + "}"));
            }
            return users;
        }

        @Override
        public String toString() {
            return String.format("User{id=%d, name='%s', email='%s'}", id, name, email);
        }
    }

    static class Post {
        private int id;
        private int userId;
        private String title;
        private String body;

        public Post(int id, int userId, String title, String body) {
            this.id = id;
            this.userId = userId;
            this.title = title;
            this.body = body;
        }

        public int getId() {
            return id;
        }

        public static List<Post> fromJsonArray(String json) {
            List<Post> posts = new ArrayList<>();
            String[] postJsons = json.replaceAll("\\[|\\]", "").split("},");
            for (String postJson : postJsons) {
                posts.add(fromJson(postJson + "}"));
            }
            return posts;
        }

        public static Post fromJson(String json) {
            String[] parts = json.replaceAll("[{}\"]", "").split(",");
            int id = Integer.parseInt(parts[0].split(":")[1].trim());
            int userId = Integer.parseInt(parts[1].split(":")[1].trim());
            String title = parts[2].split(":")[1].trim();
            String body = parts[3].split(":")[1].trim();
            return new Post(id, userId, title, body);
        }
    }

    static class Comment {
        private int id;
        private int postId;
        private String name;
        private String email;
        private String body;
        private Post post;

        public Comment(int id, int postId, String name, String email, String body) {
            this.id = id;
            this.postId = postId;
            this.name = name;
            this.email = email;
            this.body = body;
        }

        public Post getPost() {
            return post;
        }

        public static List<Comment> fromJsonArray(String json) {
            List<Comment> comments = new ArrayList<>();
            String[] commentJsons = json.replaceAll("\\[|\\]", "").split("},");
            for (String commentJson : commentJsons) {
                comments.add(fromJson(commentJson + "}"));
            }
            return comments;
        }

        public static Comment fromJson(String json) {
            String[] parts = json.replaceAll("[{}\"]", "").split(",");
            int id = Integer.parseInt(parts[0].split(":")[1].trim());
            int postId = Integer.parseInt(parts[1].split(":")[1].trim());
            String name = parts[2].split(":")[1].trim();
            String email = parts[3].split(":")[1].trim();
            String body = parts[4].split(":")[1].trim();
            return new Comment(id, postId, name, email, body);
        }

        public static String toJsonArray(List<Comment> comments) {
            StringBuilder sb = new StringBuilder("[");
            for (Comment comment : comments) {
                sb.append(comment.toJson()).append(",");
            }
            if (sb.length() > 1) {
                sb.setLength(sb.length() - 1);
            }
            sb.append("]");
            return sb.toString();
        }

        public String toJson() {
            return String.format("{\"id\": %d, \"postId\": %d, \"name\": \"%s\", \"email\": \"%s\", \"body\": \"%s\"}", id, postId, name, email, body);
        }

        @Override
        public String toString() {
            return String.format("Comment{id=%d, postId=%d, name='%s', email='%s', body='%s'}", id, postId, name, email, body);
        }
    }

    static class Todo {
        private int id;
        private int userId;
        private String title;
        private boolean completed;

        public Todo(int id, int userId, String title, boolean completed) {
            this.id = id;
            this.userId = userId;
            this.title = title;
            this.completed = completed;
        }

        public boolean isCompleted() {
            return completed;
        }

        public static List<Todo> fromJsonArray(String json) {
            List<Todo> todos = new ArrayList<>();
            String[] todoJsons = json.replaceAll("\\[|\\]", "").split("},");
            for (String todoJson : todoJsons) {
                todos.add(fromJson(todoJson + "}"));
            }
            return todos;
        }

        public static Todo fromJson(String json) {
            String[] parts = json.replaceAll("[{}\"]", "").split(",");
            int id = Integer.parseInt(parts[0].split(":")[1].trim());
            int userId = Integer.parseInt(parts[1].split(":")[1].trim());
            String title = parts[2].split(":")[1].trim();
            boolean completed = Boolean.parseBoolean(parts[3].split(":")[1].trim());
            return new Todo(id, userId, title, completed);
        }

        @Override
        public String toString() {
            return String.format("Todo{id=%d, userId=%d, title='%s', completed=%b}", id, userId, title, completed);
        }
    }
}
