precision mediump float;
attribute vec4 a_position;
attribute vec2 a_texturePosition;
varying vec2 v_texturePosition;

void main() {
    v_texturePosition = a_texturePosition;
    gl_Position = a_position;
}
