precision mediump float;
varying vec2 v_texturePosition;
uniform sampler2D u_texture;
void main() {
    gl_FragColor = texture2D(u_texture, v_texturePosition);
}
