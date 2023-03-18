import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.Arrays;
import java.util.Objects;

public class ClickableText {

    private String text;

    private HoverEvent hoverAction;

    private ClickEvent clickAction;

    public ClickableText(String text) {
        Objects.requireNonNull(text);
        this.text = text.replace("&", "§");
    }

    @SuppressWarnings("deprecation")
    public ClickableText setHoverAction(HoverEvent.Action action, String... content) {
        TextComponent[] texts = Arrays.stream(content)
                .map(e -> new TextComponent(e.replace("&", "§")))
                .toArray(TextComponent[]::new);
        this.hoverAction = new HoverEvent(action, texts);
        return this;
    }

    public ClickableText setClickAction(ClickEvent.Action action, String value) {
        this.clickAction = new ClickEvent(action, value);
        return this;
    }

    public ClickableText setText(String text) {
        this.text = text;
        return this;
    }

    public TextComponent build() {
        TextComponent component = new TextComponent();
        component.setText(this.text);
        if (this.hoverAction != null)
            component.setHoverEvent(this.hoverAction);
        if (this.clickAction != null)
            component.setClickEvent(this.clickAction);
        return component;
    }
}
