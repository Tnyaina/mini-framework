package util;

public class ApiResponse {
    private int status;
    private String message;
    private Object data;
    private Long timestamp;
    
    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public ApiResponse(int status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Factory methods
    public static ApiResponse success(Object data) {
        return new ApiResponse(200, "Success", data);
    }
    
    public static ApiResponse success(String message, Object data) {
        return new ApiResponse(200, message, data);
    }
    
    public static ApiResponse error(int status, String message) {
        return new ApiResponse(status, message, null);
    }
    
    // Getters/Setters
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}