package com.embraiz.grap.catcher;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Table {
	private Node[][] data;
	
	public Table() {
	}
	
	/**
	 * 获取单个表格节点
	 * @param row 行号 0,1,2,3...
	 * @param col 列号 0,1,2,3...
	 * @return 对应行列的表格节点
	 */
	public Node get(int row,int col){
		if(checkRange(row, col) == true){
			return data[row][col];
		}else{
			return null;
		}
		
	}
	
	/**
	 * 获取单个表格节点 不检查边界
	 * @param row
	 * @param col
	 * @return
	 */
	private Node getUncheck(int row,int col){
		return data[row][col];
	}
	
	public boolean checkRange(int row,int col){
		if(row > getRowCount() || col >= getColumnCount()){
			return false;
		}else{
			return true;
		}
	}
	
	/**
	 * 获取某一列 多行的单元格 在列上被合并过单元格 不会重复添加 只会添加一次
	 * @param col 列序号
	 * @param start 开始的航好
	 * @param len 要取的行数
	 * @return 节点List
	 */
	public List<Node> getNodes(int col,int start, int len){
		
		List<Node> nodes = new ArrayList<>();
		
		for(int i=start; i<start+len && i < getRowCount();i++){
			Node node = getUncheck(i, col);
			
			if(node == null){
				continue;
			}
			
			if(nodes.contains(node) == false){
				nodes.add(node);
			}
			
		}
		
		return nodes;
	}
	
	public static Table parse(Element table){
		Table t = new Table();
		try {
			t.parseTable(table);
		} catch (Exception e) {
			return null;
		}
		return t;
	}
	
	/**
	 * 获取对应行 列范围的所有 表格节点
	 * 如 getNodes(1,2,5,3) 会获取从第1行开始 取2行 从第5列开始 取3列 的表格节点 （注意：编号是从0开始计算的）,
	 * 即取 行号在 1-2 并且 列号在 5-7之间的所有 节点, 如果这些节点中不存在 单元格合并 则节点个数是 6个 如果存在单元合并 则会小于6.
	 * @param row 开始的行号
	 * @param rowCount 需要取出的行数
	 * @param col 开始的列号
	 * @param colCount 需要取出的列数
	 * @return 在行列范围内的所有节点
	 */
	public List<Node> getNodes(int row,int rowCount,int col,int colCount){
		List<Node> nodes = new ArrayList<>();
		for(int i = row ; i < row+rowCount && i<getRowCount(); i++){
			for( int j = col ; j < col+colCount && j<getColumnCount(); j++ ){
				Node n = getUncheck(i,j);
				if(n == null){
					continue;
				}
				
				if(nodes.contains(n) == false){
					nodes.add(n);
				}
			}
		}
		
		return nodes;
	}
	
	/**
	 * 返回表格的行数
	 * @return 表格的行数
	 */
	public int getRowCount(){
		return data.length;
	}

	/**
	 * 返回表格的列数
	 * @return 表格的列数
	 */
	public int getColumnCount(){
		return data[0].length;
	}
	
	/**
	 * 获取单行包含的节点个数
	 * @param row 行号
	 * @return
	 */
	public int getSizeByRow(int row){
		return getNodes(row, 1, 0, getColumnCount()).size();
	}
	
	/**
	 * 移除一行
	 * @param row
	 */
	public void removeRow(int row){
		removeRows(row, 1);
	}
	
	/**
	 * 移除连续的多行
	 * @param stratRow 开始的行号
	 * @param rowCount 移除的行数
	 */
	public void removeRows(int stratRow, int rowCount){
		Node[][] newData = new Node[data.length-rowCount][data[0].length];
		
		//对移除行的所有 节点 的 rowspan属性 按行 自减1
		for(int i = 0; i < rowCount; i++){
			for(Node node : getNodes( stratRow+i ,1,0,getColumnCount() ) ){
				node.rowspan--;
			}
		}
		System.arraycopy(data, 0, newData, 0, stratRow);
		System.arraycopy(data, stratRow+rowCount, newData, stratRow,  data.length - stratRow - rowCount);
		
		data = newData;
	}
	
	/**
	 * 转换表格
	 * @param table
	 * @throws Exception
	 */
	public void parseTable(Element table) throws Exception{
		
		//处理表格中的表格
		Elements tables = table.select("td table");
		for(int i=0; i< tables.size() ;i++){
			Element tb = tables.get(i);
			String text = tb.text();
			tb.parent().html(text);
		}
		
		//获取所有tr元素
		List<Element> rows = table.getElementsByTag("tr");
		//检查表格
		if(rows.size()<1){
			throw new Exception("表格不完整 转化失败");
		}
		
		//创建存储容器
		this.data = createData(table);
		
		//行合并标记
		int[] coltag = new int[data[0].length];
		
		//初始化 行合并标记 数组
		for (int i = 0; i < coltag.length; i++) {
			coltag[i] = 0;
		}
		
		//遍历所有 tr元素
		for(int i=0 ; i < rows.size() ;i++){
			//获取本行的所有td元素
			Elements cols = rows.get(i).getElementsByTag("td");
			
			//列索引号
			int colIndex = 0;
			
			//遍历本行的所有td元素
			for(int j=0 ; colIndex < coltag.length ;j++){
				boolean change = false;
				for(int k=colIndex ; k< coltag.length ;k++){
					int rowspanCounter = coltag[colIndex];
					
					//被上一行合并的单元
					if(rowspanCounter > 0){
						//设置单元节点 为同列上一行单元的节点
						set(i, colIndex, get(i-1,colIndex));
						change = true;
						//行合并计数器 自减1
						rowspanCounter--;
						coltag[colIndex] = rowspanCounter;
						
						colIndex++;
					}
					//循环到非行合并 跳出循环 从 td元素中获取 节点
					else{
						break;
					}
				}
				
				//如果已经不存在 td元素 直接跳过
				if(j >= cols.size()){
					//防止由于表格 不规范 而导致程序死锁 (表格每一行的colspan属性相加 不等)
					if(change == true)
						continue;
					else
						break;
				}
				
				
				//td元素
				Element td = cols.get(j);
				//创建节点
				Node node = new Node(td);
				//循环
				for(int k = 0; k < node.colspan ;k++){
					if(colIndex >= getColumnCount()) {
						continue;
					}
					//存储到容器中
					set(i, colIndex, node);
					
					//需要合并下面的行 设置行合并标记
					if(node.rowspan > 1){
						coltag[colIndex] = node.rowspan - 1;
					}
					
					colIndex++;
				}
			}
		}
		
		
	}
	
	/**
	 * 将单行内容装换字符串
	 * @param row 行号
	 * @return
	 */
	public String rowToText(int row){
		return rowsToText(row, 1);
	}
	
	/**
	 * 将多行内容转换为字符串
	 * @param rowStart 开始行号
	 * @param rowCount 行数
	 * @return
	 */
	public String rowsToText(int rowStart,int rowCount){
		return nodesToText(getNodes(rowStart, rowCount, 0, getColumnCount()));
	}
	

	
	protected Table(Table.Node [][] data) {
		this.data = data;
	}
	
	/**
	 * 切割表格
	 * @param stratRow 开始的行号
	 * @param rowCount 行数
	 * @return 子表格
	 */
	public Table subTable(int stratRow,int rowCount){
		Table.Node [][] data = new Node[rowCount][getColumnCount()];
		System.arraycopy(this.data, stratRow, data, 0, rowCount);
		
		return new Table(data);
	}
	
	/**
	 * 获取一个td元素的文本
	 * @param e
	 * @return
	 */
	public static String elementToText(Element e){
		return FilterFactory.filte(e.html(),"table-cell-content-filter");
	}
	
	/**
	 * 获取td元素的rowspan属性
	 * @param e
	 * @return
	 */
	public static int getRowspan(Element e){
		String attr = e.attr("rowspan");
		if(attr==null || attr.equals("")){
			return 1;
		}
		return Integer.parseInt(attr);
	}
	
	/**
	 * 获取td元素的colspan属性
	 * @param e
	 * @return
	 */
	public static int getColspan(Element e){
		String attr = e.attr("colspan");
		if(attr==null || attr.equals("")){
			return 1;
		}
		return Integer.parseInt(attr);
	}
	
	/**
	 * 将多个节点拼接成 一个字符串
	 * @param nodes
	 * @return
	 */
	public static String nodesToText(List<Node> nodes){
		StringBuilder builder = new StringBuilder();
		
		for (Node node : nodes) {
			builder.append(node.content);
		}
		
		return builder.toString();
	}
	
	/**
	 * 计算最大rowspan值
	 * @param nodes
	 * @return
	 */
	public static int getMaxRowSpan(List<Node> nodes){
		int max = 0;
		for (Node node : nodes) {
			if(node.rowspan>max){
				max = node.rowspan;
			}
		}
		
		return max;
	}
	
	/**
	 * 把表格中的表格转换为文本
	 * @param table
	 */
	public static void replaceInnerTableToText(Element table){
		//处理表格中的表格
		Elements tables = table.select("td table");
		for(int i=0; i< tables.size() ;i++){
				Element tb = tables.get(i);
				String text = tb.text();
				tb.parent().html(text);
		}
	}
	
	/**
	 * 创建与table元素对应大小 的Node[][]二维数组
	 * @param e table元素
	 * @return 二维表格节点数组
	 */
	private Node[][] createData(Element e){
		Node[][] data = null;
		
		//行计数器 列计数器
		int rowCounter = 0,colCounter = 0;
		
		Elements rows = e.getElementsByTag("tr");
		
		if(rows.size() == 0) return null;
		
		rowCounter = rows.size();
		
		Element row = rows.get(0);
		
		Elements cols = row.getElementsByTag("td");
		
		if(cols.size() == 0) return null;
		
		//累加一行中所有td元素的colspan属性
		for(int i=0;i<cols.size();i++){
			Element col = cols.get(i);
			colCounter += getColspan(col);
		}
		
		data = new Node[rowCounter][colCounter];
		data[0] = new Node[colCounter];
		
		return data;
	}
	
	/**
	 * 设置表格单元节点
	 * @param row 行号 0,1,2,3...
	 * @param col 列号 0,1,2,3..
	 * @param node 表格节点 
	 */
	private void set(int row,int col,Node node){
		this.data[row][col] = node;
	}

	public class Node{
		public String content;
		public int rowspan = 1;
		public int colspan = 1;
		
		
		
		public Node() {
			super();
		}


		
		public Node(Element td) {
			this(Table.elementToText(td),
				 Table.getRowspan(td),
				 Table.getColspan(td));
		}

		public Node(String content, int rowspan, int colspan) {
			super();
			this.content = content;
			this.rowspan = rowspan;
			this.colspan = colspan;
		}



		@Override
		public String toString() {
			return content;
		}



		
		
	}

	@Override
	public String toString() {
		
		StringBuilder builder = new StringBuilder();
		for (Node[] nodes : data) {
			for (Node node : nodes) {
				builder.append(node+"\t");
			}
			builder.append("\n");
		}
		return "Table:\n" + builder;
	}

}
