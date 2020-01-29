package com.qanairy.models.experience;

/**
 * 
 */
public class DiagnosticDetail extends AuditDetail {

	private int num_stylesheets;
	private double throughput;
	private int num_tasks_over_10ms;
	private int num_tasks_over_25ms;
	private int num_tasks_over_50ms;
	private int num_tasks_over_100ms;
	private int num_tasks_over_500ms;
	private int num_requests;
	private double total_task_time;
	private int main_document_transfer_size;
	private int total_byte_weight;
	private int num_tasks;
	private double rtt;
	private double maxRtt;
	private int numFonts;
	private int numScripts;
	
	public DiagnosticDetail(
			int num_stylesheets, 
			double throughput, 
			int num_tasks_over_10ms,
			int num_tasks_over_25ms,
			int num_tasks_over_50ms,
			int num_tasks_over_100ms,
			int num_tasks_over_500ms,
			int num_requests,
			double total_task_time,
			int main_document_transfer_size,
			int total_byte_weight,
			int num_tasks,
			double rtt,
			double max_rtt,
			int num_fonts,
			int num_scripts) {
		setNumStylesheets(num_stylesheets);
		setThroughput(throughput);
		setNumTasksOver10ms(num_tasks_over_10ms);
		setNumTasksOver25ms(num_tasks_over_25ms);
		setNumTasksOver50ms(num_tasks_over_50ms);
		setNumTasksOver100ms(num_tasks_over_100ms);
		setNumTasksOver500ms(num_tasks_over_500ms);
		setNumRequests(num_requests);
		setTotalTaskTime(total_task_time);
		setMainDocumentTransferSize(main_document_transfer_size);
		setTotalByteWeight(total_byte_weight);
		setNumTasks(num_tasks);
		setRtt(rtt);
		setMaxRtt(max_rtt);
		setNumFonts(num_fonts);
		setNumScripts(num_scripts);
	}

	public int getNumStylesheets() {
		return num_stylesheets;
	}

	public void setNumStylesheets(int num_stylesheets) {
		this.num_stylesheets = num_stylesheets;
	}

	public double getThroughput() {
		return throughput;
	}

	public void setThroughput(double throughput) {
		this.throughput = throughput;
	}

	public int getNumTasksOver10ms() {
		return num_tasks_over_10ms;
	}

	public void setNumTasksOver10ms(int num_tasks_over_10ms) {
		this.num_tasks_over_10ms = num_tasks_over_10ms;
	}

	public int getNumTasksOver25ms() {
		return num_tasks_over_25ms;
	}

	public void setNumTasksOver25ms(int num_tasks_over_25ms) {
		this.num_tasks_over_25ms = num_tasks_over_25ms;
	}

	public int getNumTasksOver50ms() {
		return num_tasks_over_50ms;
	}

	public void setNumTasksOver50ms(int num_tasks_over_50ms) {
		this.num_tasks_over_50ms = num_tasks_over_50ms;
	}

	public int getNumTasksOver100ms() {
		return num_tasks_over_100ms;
	}

	public void setNumTasksOver100ms(int num_tasks_over_100ms) {
		this.num_tasks_over_100ms = num_tasks_over_100ms;
	}

	public int getNumTasksOver500ms() {
		return num_tasks_over_500ms;
	}

	public void setNumTasksOver500ms(int num_tasks_over_500ms) {
		this.num_tasks_over_500ms = num_tasks_over_500ms;
	}

	public int getNumRequests() {
		return num_requests;
	}

	public void setNumRequests(int num_requests) {
		this.num_requests = num_requests;
	}

	public double getTotalTaskTime() {
		return total_task_time;
	}

	public void setTotalTaskTime(double total_task_time) {
		this.total_task_time = total_task_time;
	}

	public int getMainDocumentTransferSize() {
		return main_document_transfer_size;
	}

	public void setMainDocumentTransferSize(int main_document_transfer_size) {
		this.main_document_transfer_size = main_document_transfer_size;
	}

	public int getTotalByteWeight() {
		return total_byte_weight;
	}

	public void setTotalByteWeight(int total_byte_weight) {
		this.total_byte_weight = total_byte_weight;
	}

	public int getNumTasks() {
		return num_tasks;
	}

	public void setNumTasks(int num_tasks) {
		this.num_tasks = num_tasks;
	}

	public double getRtt() {
		return rtt;
	}

	public void setRtt(double rtt) {
		this.rtt = rtt;
	}

	public double getMaxRtt() {
		return maxRtt;
	}

	public void setMaxRtt(double maxRtt) {
		this.maxRtt = maxRtt;
	}

	public int getNumFonts() {
		return numFonts;
	}

	public void setNumFonts(int numFonts) {
		this.numFonts = numFonts;
	}

	public int getNumScripts() {
		return numScripts;
	}

	public void setNumScripts(int numScripts) {
		this.numScripts = numScripts;
	}
}
