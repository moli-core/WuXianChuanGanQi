package jiaju.example;

public class ChatRequest {
    private String message;
    private String sessionId;
    private Object history; // 可以改成 List<Map>

    // 必须要有无参构造器
    public ChatRequest() {}

    // Getter 和 Setter
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Object getHistory() {
        return history;
    }

    public void setHistory(Object history) {
        this.history = history;
    }
}