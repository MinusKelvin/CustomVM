package minusk.vm;

import java.io.File;
import java.io.FileInputStream;

public class VirtualMachine {
	private static byte[] memory = new byte[1<<14]; // 16 KB
	private static byte[] disk = new byte[1<<20]; // 1 MB
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
			memory[i] = disk[i];
		
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
	
	private static final int PANIC = 0x0;
	private static final int REG_TO_REG = 0x3;
	private static final int REG_TO_MEM = 0x2;
	private static final int MEM_ADDR_TO_REG = 0x1;
	private static final int MEM_NEXT_TO_REG = 0x4;
	private static final int MEM_ADDR_TO_MEM = 0x5;
	private static final int MEM_NEXT_TO_MEM = 0x6;
	private static final int REG_EQ_REG_TO_REG = 0x7;
	private static final int REG_NE_REG_TO_REG = 0x8;
	private static final int REG_GT_REG_TO_REG = 0x9;
	private static final int REG_GE_REG_TO_REG = 0xa;
	private static final int REG_LT_REG_TO_REG = 0xb;
	private static final int REG_LE_TEG_TO_REG = 0xc;
	private static final int REG_TO_REG_IF_REG = 0xd;
	private static final int OUTPUT_REG = 0xe;
	private static final int REG_BAND_REG_TO_REG = 0xf;
	private static final int REG_BOR_REG_TO_REG = 0x10;
	private static final int REG_BXOR_REG_TO_REG = 0x11;
	private static final int REG_BNOT_TO_REG = 0x12;
	private static final int REG_ADD_REG_TO_REG = 0x13;
	private static final int REG_SUB_REG_TO_REG = 0x14;
	private static final int REG_MUL_REG_TO_REG = 0x15;
	private static final int REG_DIV_REG_TO_REG = 0x16;
	private static final int REG_MOD_REG_TO_REG = 0x17;
	private static final int REG_ADD_MEM_NEXT_TO_REG = 0x18;
	private static final int REG_SUB_MEM_NEXT_TO_REG = 0x19;
	private static final int REG_MUL_MEM_NEXT_TO_REG = 0x1a;
	private static final int REG_DIV_MEM_NEXT_TO_REG = 0x1b;
	private static final int REG_MOD_MEM_NEXT_TO_REG = 0x1c;
	private static final int REG_BAND_MEM_NEXT_TO_REG = 0x1d;
	private static final int REG_BOR_MEM_NEXT_TO_REG = 0x1e;
	private static final int REG_BXOR_MEM_NEXT_TO_REG = 0x1f;
	private static final int MEM_NEXT_SUB_REG_TO_REG = 0x20;
	private static final int MEM_NEXT_DIV_REG_TO_REG = 0x21;
	private static final int MEM_NEXT_MOD_REG_TO_REG = 0x22;
	private static final int MEM_NEXT_TO_REG_IF_REG = 0x23;
//	private static final int instruction = 0x24;
//	private static final int instruction = 0x25;
//	private static final int instruction = 0x26;
//	private static final int instruction = 0x27;
//	private static final int instruction = 0x28;
//	private static final int instruction = 0x29;
//	private static final int instruction = 0x2a;
}
