package jiaju.example;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class R {
    private Boolean success;
    private String msg;
    private Map<String, Object> data = new HashMap<>();

    public static R success() {
        R r = new R();
        r.success = true;
        r.msg = "操作成功";
        return r;
    }

    public static R fail(String msg) {
        R r = new R();
        r.success = false;
        r.msg = msg;
        return r;
    }

    public R put(String key, Object val) {
        this.data.put(key, val);
        return this;
    }
}