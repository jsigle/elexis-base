package ch.elexis.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import junit.framework.TestCase;

import org.apache.commons.compress.bzip2.CBZip2InputStream;
import org.apache.commons.compress.bzip2.CBZip2OutputStream;

import ch.rgw.compress.CompEx;
import ch.rgw.compress.GLZInputStream;
import ch.rgw.compress.GLZOutputStream;
import ch.rgw.compress.HuffmanInputStream;
import ch.rgw.compress.HuffmanOutputStream;
import ch.rgw.compress.HuffmanTree;
import ch.rgw.compress.RLL;

public class TestCompress extends TestCase {
	byte[] in, out;
	ByteArrayInputStream bais;
	ByteArrayOutputStream baos;
	
	protected void setUp() throws Exception{
		in = new byte[2048];
		for (int i = 0; i < 256; i++) {
			in[i] = (byte) i;
			in[256 + i] = (byte) i;
			in[512 + i] = (byte) i;
			in[768 + i] = (byte) Math.round(256 * Math.random());
			in[1024 + i] = 25;
			in[1280 + i] = (byte) ((64 + i) % 127);
			in[1536 + i] = (byte) Math.round(64 + 64 * Math.random());
			in[1792 + i] = 0;
		}
		
	}
	
	public void testGLZ() throws Exception{
		long start = System.currentTimeMillis();
		GLZOutputStream gous;
		GLZInputStream gis;
		
		baos = new ByteArrayOutputStream(1024);
		gous = new GLZOutputStream(baos);
		for (int i = 0; i < in.length; i++) {
			gous.write(in[i]);
		}
		gous.close();
		out = baos.toByteArray();
		
		bais = new ByteArrayInputStream(out);
		gis = new GLZInputStream(bais);
		for (int i = 0; i < in.length; i++) {
			assertEquals((int) in[i], (byte) gis.read());
		}
		long end = System.currentTimeMillis();
		System.out.println("GLZ time: " + Long.toString(end - start) + ", size: " + out.length);
		
	}
	
	public void testZip() throws Exception{
		long start = System.currentTimeMillis();
		
		ZipOutputStream zos;
		ZipInputStream zis;
		
		baos = new ByteArrayOutputStream(1024);
		zos = new ZipOutputStream(baos);
		zos.putNextEntry(new ZipEntry("Data"));
		for (int i = 0; i < in.length; i++) {
			zos.write(in[i]);
		}
		zos.close();
		out = baos.toByteArray();
		
		bais = new ByteArrayInputStream(out);
		zis = new ZipInputStream(bais);
		zis.getNextEntry();
		for (int i = 0; i < in.length; i++) {
			assertEquals((int) in[i], (byte) zis.read());
		}
		long end = System.currentTimeMillis();
		System.out.println("ZIP time: " + Long.toString(end - start) + ", size: " + out.length);
	}
	
	public void testBZIP2() throws Exception{
		long start = System.currentTimeMillis();
		CBZip2InputStream cis;
		CBZip2OutputStream cos;
		baos = new ByteArrayOutputStream(1024);
		cos = new CBZip2OutputStream(baos);
		for (int i = 0; i < in.length; i++) {
			cos.write(in[i]);
		}
		cos.close();
		out = baos.toByteArray();
		
		bais = new ByteArrayInputStream(out);
		cis = new CBZip2InputStream(bais);
		for (int i = 0; i < in.length; i++) {
			assertEquals(in[i], (byte) cis.read());
		}
		long end = System.currentTimeMillis();
		System.out.println("BZIP2 time: " + Long.toString(end - start) + ", size: " + out.length);
		
	}
	
	public void testHuff() throws Exception{
		long start = System.currentTimeMillis();
		
		HuffmanTree tree = new HuffmanTree(in);
		HuffmanInputStream his;
		HuffmanOutputStream hos;
		baos = new ByteArrayOutputStream(1024);
		hos = new HuffmanOutputStream(baos, tree, 0);
		for (int i = 0; i < in.length; i++) {
			hos.write(in[i]);
		}
		hos.close();
		out = baos.toByteArray();
		
		bais = new ByteArrayInputStream(out);
		his = new HuffmanInputStream(bais);
		for (int i = 0; i < in.length; i++) {
			assertEquals(in[i], (byte) his.read());
		}
		long end = System.currentTimeMillis();
		System.out.println("Huff time: " + Long.toString(end - start) + ", size: " + out.length);
		
	}
	
	public void testRLL() throws Exception{
		long start = System.currentTimeMillis();
		out = RLL.compress((byte) 26, in);
		byte[] test = RLL.expand(out);
		assertTrue(Arrays.equals(in, test));
		long end = System.currentTimeMillis();
		System.out.println("RLL time: " + Long.toString(end - start) + ", size: " + out.length);
		
	}
	
	public void testCompEx() throws Exception{
		byte[] test = CompEx.Compress(in, CompEx.ZIP);
		byte[] ex = CompEx.expand(test);
		assertTrue(Arrays.equals(in, ex));
		
		test = CompEx.Compress(in, CompEx.BZIP2);
		ex = CompEx.expand(test);
		assertTrue(Arrays.equals(in, ex));
		test = CompEx.Compress(in, CompEx.ZIP);
		ex = CompEx.expand(test);
		assertTrue(Arrays.equals(in, ex));
		test = CompEx.Compress(in, CompEx.GLZ);
		ex = CompEx.expand(test);
		assertTrue(Arrays.equals(in, ex));
		/*
		 * test=CompEx.Compress(in,CompEx.HUFF); ex=CompEx.expand(test);
		 * assertTrue(Arrays.equals(in,ex));
		 * 
		 * test=CompEx.Compress(in,CompEx.RLL); ex=CompEx.expand(test);
		 * assertTrue(Arrays.equals(in,ex));
		 */
	}
}
