package io.rong.imkit;

import java.util.List;


public class InputMenu {
    public String title;
    public List<String> subMenuList;

    public static InputMenu obtain() {
        return new InputMenu();
    }
}
