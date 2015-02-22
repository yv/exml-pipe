package de.versley.exml.annotators.preprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Queues;

public class LineBasedWrapper implements LineProcessor {
	public List<String> cmd;
	protected Queue<LineConsumer> _consumers =
			Queues.newArrayDeque();
	protected Thread _gobbler = null;
	private Process _proc = null;
	private BufferedWriter _proc_out = null;
	private BufferedReader _proc_in = null;
	
	
	public LineBasedWrapper() {}
	
	public LineBasedWrapper(Collection<String> parts) {
		cmd = new ArrayList<String>();
		cmd.addAll(parts);
	}


	@Override
	public void loadModels() {
		ProcessBuilder pb = new ProcessBuilder(cmd);
		try {
			pb.redirectError(ProcessBuilder.Redirect.INHERIT);
			_proc = pb.start();
			_proc_out = new BufferedWriter(
				new OutputStreamWriter(_proc.getOutputStream()));
			_proc_in = new BufferedReader(new InputStreamReader(_proc.getInputStream()));
			// Workaround: Java wraps the streams in a BufferedStream,
			//  which is absolutely horrible if we want to read single lines
			_gobbler = new Thread(new Runnable() {
				public void run() {
					while(true) {
						String s;
						try {
							System.err.println("Gobbler: waiting for line");
							s = _proc_in.readLine();
							synchronized(_consumers) {
								LineConsumer c = _consumers.remove();
								c.consume(s);
							}
						} catch (IOException ex) {
							// premature end, no lines left
							System.err.println("no output left");
							ex.printStackTrace();
							break;
						} catch (NoSuchElementException ex) {
							// extra line, no consumer
							System.err.println("no consumer left");
							break;
						}
					}
				}
			});
		_gobbler.start();
		} catch (IOException ex) {
			throw new RuntimeException("Cannot start:"+StringUtils.join(" ", cmd), ex);
		}
	}


	@Override
	public void preprocess_line(String input, LineConsumer and_then) {
		try {
			synchronized (_consumers) {
				_consumers.add(and_then);
				_proc_out.write(input);
				if (!input.endsWith("\n")) {
					_proc_out.newLine();
				}
				_proc_out.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot interact:"+StringUtils.join(cmd, " ")+"/Input:"+input, e);
		}
		
	}
	

	@Override
	public void close() {
		try {
			_proc_out.close();
			_proc.waitFor();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		String cmd="tr a-z L-ZA-K";
		LineBasedWrapper wrap = new LineBasedWrapper(Arrays.asList(cmd.split(" ")));
		wrap.loadModels();
		for (String s: new String[]{"hello world", "goodbye world"}) {
			final String ss = s;
			wrap.preprocess_line(s, new LineConsumer() {
				@Override
				public void consume(String line) {
					System.out.println("Input: "+ss);
					System.out.println("Output:"+line);
				}
			});
		}
		wrap.close();
	}

	
}
