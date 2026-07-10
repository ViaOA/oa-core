package com.viaoa.callback;

/**
 * Mutable display carrier used by OA callback processing.
 * <p>
 * Rule and UI callback code can populate this object with label text and
 * presentation hints. The owning UI/controller decides how these values are
 * applied; this class does not enforce visibility, enabled state, or styling by
 * itself.
 * </p>
 */
public class OACallbackLabel {
    /** Text to display for the label. */
    private String text;
    /** Tooltip or hover text associated with the label. */
    private String tooltip;
    /** Generic CSS-like style string. */
    private String style;        // generic CSS-like style string
    /** Foreground color, usually a named value or {@code #RRGGBB}. */
    private String color;        // "#RRGGBB" or named token
    /** Background color, usually a named value or {@code #RRGGBB}. */
    private String background;
    /** Font family name or token. */
    private String fontFamily;
    /** Font size value such as {@code 12px}, {@code 1rem}, or {@code small}. */
    private String fontSize;     // "12px", "1rem", "small", etc.
    /** Font weight value such as {@code normal} or {@code bold}. */
    private String fontWeight;   // "normal", "bold"
    /** Horizontal alignment such as {@code left}, {@code center}, or {@code right}. */
    private String align;        // "left", "center", "right"
    /** Preferred label width, interpreted by the owning UI. */
    private int width;
    /** Preferred label height, interpreted by the owning UI. */
    private int height;
    /** Display visibility hint for the owning UI. */
    private boolean visible = true;
    /** Enabled-state hint for the owning UI. */
    private boolean enabled = true;
    
    
    
	/**
	 * Returns the label text.
	 *
	 * @return label text, or {@code null}
	 */
	public String getText() {
		return text;
	}
	/**
	 * Sets the label text.
	 *
	 * @param text label text, or {@code null}
	 */
	public void setText(String text) {
		this.text = text;
	}
	/**
	 * Returns tooltip text for the label.
	 *
	 * @return tooltip text, or {@code null}
	 */
	public String getTooltip() {
		return tooltip;
	}
	/**
	 * Sets tooltip text for the label.
	 *
	 * @param tooltip tooltip text, or {@code null}
	 */
	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}
	/**
	 * Returns the generic style string.
	 *
	 * @return style string, or {@code null}
	 */
	public String getStyle() {
		return style;
	}
	/**
	 * Sets the generic style string.
	 *
	 * @param style style string, or {@code null}
	 */
	public void setStyle(String style) {
		this.style = style;
	}
	/**
	 * Returns the foreground color hint.
	 *
	 * @return color value, or {@code null}
	 */
	public String getColor() {
		return color;
	}
	/**
	 * Sets the foreground color hint.
	 *
	 * @param color color value, or {@code null}
	 */
	public void setColor(String color) {
		this.color = color;
	}
	/**
	 * Returns the background color hint.
	 *
	 * @return background color value, or {@code null}
	 */
	public String getBackground() {
		return background;
	}
	/**
	 * Sets the background color hint.
	 *
	 * @param background background color value, or {@code null}
	 */
	public void setBackground(String background) {
		this.background = background;
	}
	/**
	 * Returns the font family hint.
	 *
	 * @return font family, or {@code null}
	 */
	public String getFontFamily() {
		return fontFamily;
	}
	/**
	 * Sets the font family hint.
	 *
	 * @param fontFamily font family, or {@code null}
	 */
	public void setFontFamily(String fontFamily) {
		this.fontFamily = fontFamily;
	}
	/**
	 * Returns the font size hint.
	 *
	 * @return font size, or {@code null}
	 */
	public String getFontSize() {
		return fontSize;
	}
	/**
	 * Sets the font size hint.
	 *
	 * @param fontSize font size, or {@code null}
	 */
	public void setFontSize(String fontSize) {
		this.fontSize = fontSize;
	}
	/**
	 * Returns the font weight hint.
	 *
	 * @return font weight, or {@code null}
	 */
	public String getFontWeight() {
		return fontWeight;
	}
	/**
	 * Sets the font weight hint.
	 *
	 * @param fontWeight font weight, or {@code null}
	 */
	public void setFontWeight(String fontWeight) {
		this.fontWeight = fontWeight;
	}
	/**
	 * Returns the horizontal alignment hint.
	 *
	 * @return alignment value, or {@code null}
	 */
	public String getAlign() {
		return align;
	}
	/**
	 * Sets the horizontal alignment hint.
	 *
	 * @param align alignment value, or {@code null}
	 */
	public void setAlign(String align) {
		this.align = align;
	}
	/**
	 * Returns the preferred width.
	 *
	 * @return preferred width
	 */
	public int getWidth() {
		return width;
	}
	/**
	 * Sets the preferred width.
	 *
	 * @param width preferred width
	 */
	public void setWidth(int width) {
		this.width = width;
	}
	/**
	 * Returns the preferred height.
	 *
	 * @return preferred height
	 */
	public int getHeight() {
		return height;
	}
	/**
	 * Sets the preferred height.
	 *
	 * @param height preferred height
	 */
	public void setHeight(int height) {
		this.height = height;
	}
	/**
	 * Returns the display visibility hint.
	 *
	 * @return {@code true} when the label should be visible
	 */
	public boolean isVisible() {
		return visible;
	}
	/**
	 * Sets the display visibility hint.
	 *
	 * @param visible {@code true} when the label should be visible
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	/**
	 * Returns the enabled-state hint.
	 *
	 * @return {@code true} when the label should be enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}
	/**
	 * Sets the enabled-state hint.
	 *
	 * @param enabled {@code true} when the label should be enabled
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
    
    
    
    
}

