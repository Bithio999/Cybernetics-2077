package dev.cybernetics2077.client.temporal;

import org.lwjgl.opengl.*;

/**
 * Native sprite uploads leave row/skip offsets behind. Raw frame/float uploads must isolate them.
 */
final class TextureUnpack implements AutoCloseable {
  private static final int[] KEYS = {
    GL11.GL_UNPACK_ALIGNMENT,
    GL11.GL_UNPACK_ROW_LENGTH,
    GL11.GL_UNPACK_SKIP_PIXELS,
    GL11.GL_UNPACK_SKIP_ROWS,
    GL12.GL_UNPACK_IMAGE_HEIGHT,
    GL12.GL_UNPACK_SKIP_IMAGES
  };
  private final int[] previous = new int[KEYS.length];

  TextureUnpack() {
    for (int i = 0; i < KEYS.length; i++) {
      previous[i] = GL11.glGetInteger(KEYS[i]);
      GL11.glPixelStorei(KEYS[i], i == 0 ? 4 : 0);
    }
  }

  @Override
  public void close() {
    for (int i = 0; i < KEYS.length; i++) GL11.glPixelStorei(KEYS[i], previous[i]);
  }
}
