package github.codeReaper2001.enums;

import lombok.Getter;

// 压缩类型枚举
@Getter
public enum CompressTypeEnum {
    GZIP((byte) 0x01, "gzip");

    private final byte code;
    private final String name;

    CompressTypeEnum(byte code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getName(byte compressCode) {
        for (CompressTypeEnum c : CompressTypeEnum.values()) {
            if (c.code == compressCode) {
                return c.name;
            }
        }
        return null;
    }
}
