package minusk.vm;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_RGBA8;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glTexSubImage2D;
import static org.lwjgl.opengl.GL12.GL_BGRA;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.io.File;
import java.io.FileInputStream;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GLContext;

public class VirtualMachine {
	private static byte[] memory = new byte[1<<24]; // 16 MB
	private static byte[] disk = new byte[1<<30]; // 1 GB
	private static int[] registers = new int[8];
	// Register 7 is the instruction pointer. Jumps are done by writing to Register 7
	
	public static void main(String[] args) {
		File diskImage = new File("disk.dim");
		try (FileInputStream in = new FileInputStream(diskImage)) {
			in.read(disk);
		} catch (Exception e) {
			System.err.println("Could not read disk image");
			return;
		}
		
		for (int i = 0; i < 512; i++)
			memory[i+1638400] = disk[i];
		registers[7] = 1638400;
		
		glfwInit();
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, 1);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		long win = glfwCreateWindow(640, 480, "CustomVM", NULL, NULL);
		glfwMakeContextCurrent(win);
		glfwSwapInterval(0);
		GLContext.createFromCurrent();
		
		glBindVertexArray(glGenVertexArrays());
		int tex = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, tex);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 640, 480, 0, GL_BGRA, GL_FLOAT, 0);
		IntBuffer buffer = BufferUtils.createIntBuffer(640*480);
		int s1 = glCreateShader(GL_VERTEX_SHADER), s2 = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(s1, "#version 330 core\nconst vec2[] verts=vec2[4](vec2(-1.0,-1.0),vec2(1.0,-1.0),vec2(-1.0,1.0),vec2(1.0,1.0));"
				+ "out vec2 tex;void main(){gl_Position=vec4(verts[gl_VertexID],0.0,1.0);tex=verts[gl_VertexID]/2.0;tex.x+=0.5;tex.y+=0.5;}");
		glShaderSource(s2, "#version 330 core\nin vec2 tex;out vec4 col;uniform sampler2D textures;void main(){col=texture(textures,tex);}");
		glCompileShader(s1);
		glCompileShader(s2);
		int prog = glCreateProgram();
		glAttachShader(prog, s1);
		glAttachShader(prog, s2);
		glLinkProgram(prog);
		glUseProgram(prog);
		glDeleteShader(s2);
		glDeleteShader(s1);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		
		while (true) {
//			System.out.println("Exec 0x" + Integer.toHexString(registers[7]).toUpperCase());
			switch (memory[registers[7]++]) {
			case PANIC:
				System.out.println("Hit byte 0 at 0x" + Integer.toHexString(registers[7]-1).toUpperCase());
				return;
			case MEM_ADDR_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				registers[reg1] = getDWord(registers[reg2]);
			}
				break;
			case REG_TO_MEM:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				memory[registers[reg1]] = (byte) (registers[reg2] & 0xff);
				memory[registers[reg1]+1] = (byte) (registers[reg2] >> 8 & 0xff);
				memory[registers[reg1]+2] = (byte) (registers[reg2] >> 16 & 0xff);
				memory[registers[reg1]+3] = (byte) (registers[reg2] >> 24 & 0xff);
			}
				break;
			case REG_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				registers[reg1] = registers[reg2];
			}
				break;
			case MEM_ADDR_TO_MEM:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				memory[registers[reg1]] = memory[registers[reg2]];
			}
				break;
			case REG_EQ_REG_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				int reg3 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] == registers[reg3] ? 1 : 0;
			}
				break;
			case REG_NE_REG_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				int reg3 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] != registers[reg3] ? 1 : 0;
			}
				break;
			case REG_LT_REG_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				int reg3 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] < registers[reg3] ? 1 : 0;
			}
				break;
			case REG_LE_TEG_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				int reg3 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] <= registers[reg3] ? 1 : 0;
			}
				break;
			case REG_GT_REG_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				int reg3 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] > registers[reg3] ? 1 : 0;
			}
				break;
			case REG_GE_REG_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				int reg3 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] >= registers[reg3] ? 1 : 0;
			}
				break;
			case REG_TO_REG_IF_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				int reg3 = getByte(registers[7]++);
				if (registers[reg3] != 0)
					registers[reg1] = registers[reg2];
			}
				break;
			case MEM_NEXT_TO_REG:
			{
				int reg = getByte(registers[7]++);
				registers[reg] = getDWord((registers[7]+=4)-4);
			}
				break;
			case MEM_NEXT_TO_MEM:
			{
				int reg = getByte(registers[7]++);
				memory[registers[reg]] = (byte) getByte(registers[7]++);
			}
				break;
			case OUTPUT_REG:
			{
				int reg = getByte(registers[7]++);
				System.out.print((char) registers[reg]);
			}
				break;
			case REG_BAND_REG_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				int reg3 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] & registers[reg3];
			}
				break;
			case REG_BOR_REG_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				int reg3 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] | registers[reg3];
			}
				break;
			case REG_BXOR_REG_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				int reg3 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] ^ registers[reg3];
			}
				break;
			case REG_BNOT_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				registers[reg1] = ~registers[reg2];
			}
				break;
			case REG_ADD_REG_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				int reg3 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] + registers[reg3];
			}
				break;
			case REG_SUB_REG_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				int reg3 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] - registers[reg3];
			}
				break;
			case REG_MUL_REG_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				int reg3 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] * registers[reg3];
			}
				break;
			case REG_DIV_REG_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				int reg3 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] / registers[reg3];
			}
				break;
			case REG_MOD_REG_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				int reg3 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] % registers[reg3];
			}
				break;
			case REG_ADD_MEM_NEXT_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] + getDWord((registers[7]+=4)-4);
			}
				break;
			case REG_SUB_MEM_NEXT_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] - getDWord((registers[7]+=4)-4);
			}
				break;
			case REG_MUL_MEM_NEXT_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] * getDWord((registers[7]+=4)-4);
			}
				break;
			case REG_DIV_MEM_NEXT_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] / getDWord((registers[7]+=4)-4);
			}
				break;
			case REG_MOD_MEM_NEXT_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] % getDWord((registers[7]+=4)-4);
			}
				break;
			case MEM_NEXT_SUB_REG_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				registers[reg1] = getDWord((registers[7]+=4)-4) - registers[reg2];
			}
				break;
			case MEM_NEXT_DIV_REG_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				registers[reg1] = getDWord((registers[7]+=4)-4) / registers[reg2];
			}
				break;
			case MEM_NEXT_MOD_REG_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				registers[reg1] = getDWord((registers[7]+=4)-4) % registers[reg2];
			}
				break;
			case REG_BAND_MEM_NEXT_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] & getDWord((registers[7]+=4)-4);
			}
				break;
			case REG_BOR_MEM_NEXT_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] | getDWord((registers[7]+=4)-4);
			}
				break;
			case REG_BXOR_MEM_NEXT_TO_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				registers[reg1] = registers[reg2] ^ getDWord((registers[7]+=4)-4);
			}
				break;
			case MEM_NEXT_TO_REG_IF_REG:
			{
				int reg1 = getByte(registers[7]++);
				int reg2 = getByte(registers[7]++);
				registers[7]+=4;
				if (registers[reg2] != 0)
					registers[reg1] = getDWord(registers[7]-4);
			}
				break;
			case UPDATE_SCREEN:
				buffer.position(0);
				for (int i = 0; i < 640*480; i++)
					buffer.put(getDWord(i*4));
				buffer.position(0);
				glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 640, 480, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
				glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
				glfwSwapBuffers(win);
				glfwPollEvents();
				break;
			}
		}
	}
	
	private static int getByte(int address) {
		return (int) memory[address] & 0xff;
	}
	
	private static int getWord(int address) {
		return getByte(address) | getByte(address+1) << 8;
	}
	
	private static int getDWord(int address) {
		return getWord(address) | getWord(address+2) << 16;
	}
	
	private static final int PANIC =							0x00;
	private static final int MEM_ADDR_TO_REG =					0x01;
	private static final int REG_TO_MEM =						0x02;
	private static final int REG_TO_REG =						0x03;
	private static final int MEM_NEXT_TO_REG =					0x04;
	private static final int MEM_ADDR_TO_MEM =					0x05;
	private static final int MEM_NEXT_TO_MEM =					0x06;
	private static final int REG_EQ_REG_TO_REG =				0x07;
	private static final int REG_NE_REG_TO_REG =				0x08;
	private static final int REG_GT_REG_TO_REG =				0x09;
	private static final int REG_GE_REG_TO_REG =				0x0a;
	private static final int REG_LT_REG_TO_REG =				0x0b;
	private static final int REG_LE_TEG_TO_REG =				0x0c;
	private static final int REG_TO_REG_IF_REG =				0x0d;
	private static final int OUTPUT_REG =						0x0e;
	private static final int REG_BAND_REG_TO_REG =				0x0f;
	private static final int REG_BOR_REG_TO_REG =				0x10;
	private static final int REG_BXOR_REG_TO_REG =				0x11;
	private static final int REG_BNOT_TO_REG =					0x12;
	private static final int REG_ADD_REG_TO_REG =				0x13;
	private static final int REG_SUB_REG_TO_REG =				0x14;
	private static final int REG_MUL_REG_TO_REG =				0x15;
	private static final int REG_DIV_REG_TO_REG =				0x16;
	private static final int REG_MOD_REG_TO_REG =				0x17;
	private static final int REG_ADD_MEM_NEXT_TO_REG =			0x18;
	private static final int REG_SUB_MEM_NEXT_TO_REG =			0x19;
	private static final int REG_MUL_MEM_NEXT_TO_REG =			0x1a;
	private static final int REG_DIV_MEM_NEXT_TO_REG =			0x1b;
	private static final int REG_MOD_MEM_NEXT_TO_REG =			0x1c;
	private static final int REG_BAND_MEM_NEXT_TO_REG =			0x1d;
	private static final int REG_BOR_MEM_NEXT_TO_REG =			0x1e;
	private static final int REG_BXOR_MEM_NEXT_TO_REG =			0x1f;
	private static final int MEM_NEXT_SUB_REG_TO_REG =			0x20;
	private static final int MEM_NEXT_DIV_REG_TO_REG =			0x21;
	private static final int MEM_NEXT_MOD_REG_TO_REG =			0x22;
	private static final int MEM_NEXT_TO_REG_IF_REG =			0x23;
	private static final int UPDATE_SCREEN =					0x24;
//	private static final int instruction = 0x25;
//	private static final int instruction = 0x26;
//	private static final int instruction = 0x27;
//	private static final int instruction = 0x28;
//	private static final int instruction = 0x29;
//	private static final int instruction = 0x2a;
}
