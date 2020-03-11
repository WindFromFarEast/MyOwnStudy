//
// Created by xiangweixin on 2020-03-08.
//

#ifndef MYOWNSTUDY_GLUTILS_H
#define MYOWNSTUDY_GLUTILS_H

#include <GLES2/gl2.h>

class GLUtils {
public:
    static int loadShader(GLenum shaderType, const char *shaderCode, int &shaderId);
    static int createProgram(const char* vertexShaderCode, const char *fragmentShaderCode, int &programId);
};


#endif //MYOWNSTUDY_GLUTILS_H
