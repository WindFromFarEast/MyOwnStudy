//
// Created by xiangweixin on 2020-03-08.
//

#include <Result.h>
#include "../header/GLUtils.h"

int GLUtils::createProgram(const char *vertexShaderCode, const char *fragmentShaderCode,
                            int &programId) {
    int vertexShaderId;
    int ret = loadShader(GL_VERTEX_SHADER, vertexShaderCode, vertexShaderId);
    if (ret != 0) {
        return ret;
    }
    int fragmentShaderId;
    ret = loadShader(GL_FRAGMENT_SHADER, fragmentShaderCode, fragmentShaderId);
    if (ret != 0) {
        return ret;
    }
    int progId = glCreateProgram();
    if (progId != 0) {
        glAttachShader(progId, vertexShaderId);
        glAttachShader(progId, fragmentShaderId);
        glLinkProgram(progId);
        GLint linkStatus;
        glGetProgramiv(progId, GL_LINK_STATUS, &linkStatus);
        if (linkStatus == GL_FALSE) {
            glDeleteShader(vertexShaderId);
            glDeleteShader(fragmentShaderId);
            glDeleteProgram(progId);
        }
        programId = progId;
        return SUCCESS;
    }
    return RECORDER_CREATE_PROGRAME_FAILED;
}

int GLUtils::loadShader(GLenum shaderType, const char *shaderCode, int &shaderId) {
    int shader = glCreateShader(shaderType);
    if (shader != 0) {
        glShaderSource(shader, 1, &shaderCode, 0);
        glCompileShader(shader);
        GLint compileStatus;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compileStatus);
        if (compileStatus == GL_FALSE) {
            glDeleteShader(shader);
            return RECORDER_GL_COMPILE_FAILED;
        }
        shaderId = shader;
        return SUCCESS;
    }
    return RECORDER_LOAD_SHADER_FAILED;
}