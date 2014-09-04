package com.sfpay.sfgo.common.util;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExcelUtil {

	private static Logger logger = LoggerFactory.getLogger(ExcelUtil.class);

	/**
	 * 创建excel workbook
	 * 
	 * @param titles
	 * @param rowData
	 * @param sheetName
	 * @param autoSizeColumn
	 *            自动适应宽带的列号
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static void generateExcel(String[] titles, List<String[]> rowData,
			String sheetName, int[] autoSizeColumn, String fileName,
			HttpServletResponse response) {
		if (null == titles || null == rowData) {
			return;
		}
		// 创建新的Excel 工作簿
		HSSFWorkbook workbook = new HSSFWorkbook();
		// 在Excel工作簿中建一工作表，其名为缺省值, 也可以指定Sheet名称
		HSSFSheet sheet = workbook.createSheet(sheetName);
		// 设置默认宽度
		sheet.setDefaultColumnWidth(12);
		// 字体高度
		HSSFFont font = workbook.createFont();
		font.setFontHeightInPoints((short) 12);
		// title单元格样式
		HSSFCellStyle titleCellStyle = getDefaultHSSFTitleStyle(workbook, font);
		// 数据单元格样式
		HSSFCellStyle cellStyle = getDefaultHSSFCellStyle(workbook, font);
		// 行号
		int rowNum = 0;
		// 创建title row,并将titles放入其中. 行号从0开始计算.
		HSSFRow titleRow = sheet.createRow((short) rowNum);
		rowNum = rowNum + 1;

		int titLength = titles.length;
		for (int i = 0; i < titLength; i++) {
			HSSFCell cell = titleRow.createCell((short) i);
			HSSFRichTextString titleString = new HSSFRichTextString(titles[i]);
			cell.setCellValue(titleString);
			cell.setCellStyle(titleCellStyle);
		}
		// 数据行
		int rowSize = rowData.size();
		for (int i = 0; i < rowSize; i++) {
			HSSFRow dataRow = sheet.createRow((short) rowNum);
			rowNum = rowNum + 1;
			int cellIndex = 0;
			for (String data : rowData.get(i)) {
				HSSFCell cell = dataRow.createCell((short) cellIndex);
				cellIndex = cellIndex + 1;
				HSSFRichTextString titleString = new HSSFRichTextString(data);
				cell.setCellValue(titleString);
				cell.setCellStyle(cellStyle);
			}
		}

		// 设置列宽自动调整，数字代笔列号，必须在单元格生成之后再设置
		for (int column : autoSizeColumn) {
			sheet.autoSizeColumn((short) column);
		}
		// 下载excel
		output(workbook, fileName, response);

	}

	public static String getHSSFCellFormatValue(HSSFRow row, int column) {
		return getHSSFCellFormatValue(row.getCell(column)).trim();
	}

	public static String getXSSFCellFormatValue(XSSFRow row, int column) {
		return getXSSFCellFormatValue(row.getCell(column)).trim();
	}

	public static String getXSSFCellValue(XSSFRow row, int column)
			throws Exception {
		String cellvalue = StringUtils.EMPTY;
		XSSFCell cell = row.getCell(column);
		if (cell != null) {
			switch (cell.getCellType()) {
			case XSSFCell.CELL_TYPE_BOOLEAN:
				cellvalue = String.valueOf(cell.getBooleanCellValue());
				break;
			case XSSFCell.CELL_TYPE_FORMULA:
				// 判断当前的cell是否为Date
				if (HSSFDateUtil.isCellDateFormatted(cell)) {
					return DateUtil.date2Str(cell.getDateCellValue(),
							DateUtil.NORMAL_DATE_FORMAT);
				} else {
					cellvalue = getNumericCellStringValue(cell
							.getNumericCellValue());
				}
				break;
			case XSSFCell.CELL_TYPE_NUMERIC:
				if (HSSFDateUtil.isCellDateFormatted(cell)) {
					return DateUtil.date2Str(cell.getDateCellValue(),
							DateUtil.NORMAL_DATE_FORMAT);
				} else {
					cellvalue = getNumericCellStringValue(cell
							.getNumericCellValue());
				}
				break;
			case XSSFCell.CELL_TYPE_STRING:
				cellvalue = cell.getStringCellValue();
				break;
			case XSSFCell.CELL_TYPE_BLANK:
				break;
			case XSSFCell.CELL_TYPE_ERROR:
				break;
			}
		}
		return cellvalue;
	}

	public static String getHSSFCellValue(HSSFRow row, int column)
			throws Exception {
		String cellvalue = StringUtils.EMPTY;
		HSSFCell cell = row.getCell(column);
		if (cell != null) {
			switch (cell.getCellType()) {
			case HSSFCell.CELL_TYPE_BOOLEAN:
				cellvalue = String.valueOf(cell.getBooleanCellValue());
				break;
			case HSSFCell.CELL_TYPE_FORMULA:
				// 判断当前的cell是否为Date
				if (HSSFDateUtil.isCellDateFormatted(cell)) {
					return DateUtil.date2Str(cell.getDateCellValue(),
							DateUtil.NORMAL_DATE_FORMAT);
				} else {
					cellvalue = getNumericCellStringValue(cell
							.getNumericCellValue());
				}
				break;
			case HSSFCell.CELL_TYPE_NUMERIC:
				if (HSSFDateUtil.isCellDateFormatted(cell)) {
					return DateUtil.date2Str(cell.getDateCellValue(),
							DateUtil.NORMAL_DATE_FORMAT);
				} else {
					cellvalue = getNumericCellStringValue(cell
							.getNumericCellValue());
				}
				break;
			case HSSFCell.CELL_TYPE_STRING:
				cellvalue = cell.getStringCellValue();
				break;
			case HSSFCell.CELL_TYPE_BLANK:
				break;
			case HSSFCell.CELL_TYPE_ERROR:
				break;
			}
		}
		return cellvalue;
	}

	private static String getNumericCellStringValue(Double value) {
		BigDecimal decimal = new BigDecimal(value);
		return String.valueOf(decimal);
	}

	/**
	 * 根据HSSFCell类型设置数据
	 * 
	 * @param cell
	 * @return
	 */
	private static String getHSSFCellFormatValue(HSSFCell cell) {
		String cellvalue = "";
		if (cell != null) {
			// 判断当前Cell的Type
			switch (cell.getCellType()) {
			// 如果当前Cell的Type为NUMERIC
			case HSSFCell.CELL_TYPE_NUMERIC:
			case HSSFCell.CELL_TYPE_FORMULA: {
				// 判断当前的cell是否为Date
				if (HSSFDateUtil.isCellDateFormatted(cell)) {
					// TODO 测试getDateCellValue的返回值
					return DateUtil.date2Str(cell.getDateCellValue(),
							DateUtil.NORMAL_DATE_FORMAT);
					// .toLocaleString();
				} else {
					cellvalue = String.valueOf(cell.getNumericCellValue());
				}
				break;
			}
			case HSSFCell.CELL_TYPE_STRING:
				cellvalue = cell.getRichStringCellValue().getString();
				break;
			default:
				cellvalue = StringUtils.EMPTY;
			}
		} else {
			cellvalue = StringUtils.EMPTY;
		}
		return cellvalue.trim();

	}

	/**
	 * 根据XSSFCell类型设置数据
	 * 
	 * @param cell
	 * @return
	 */
	private static String getXSSFCellFormatValue(XSSFCell cell) {
		String cellvalue = "";
		if (cell != null) {
			// 判断当前Cell的Type
			switch (cell.getCellType()) {
			// 如果当前Cell的Type为NUMERIC
			case HSSFCell.CELL_TYPE_NUMERIC:
			case HSSFCell.CELL_TYPE_FORMULA: {
				// 判断当前的cell是否为Date
				if (HSSFDateUtil.isCellDateFormatted(cell)) {
					// TODO 测试getDateCellValue的返回值
					return DateUtil.date2Str(cell.getDateCellValue(),
							DateUtil.NORMAL_DATE_FORMAT);
					// .toLocaleString();
				} else {
					cellvalue = String.valueOf(cell.getNumericCellValue());
				}
				break;
			}
			case HSSFCell.CELL_TYPE_STRING:
				cellvalue = cell.getRichStringCellValue().getString();
				break;
			default:
				cellvalue = StringUtils.EMPTY;
			}
		} else {
			cellvalue = StringUtils.EMPTY;
		}
		return cellvalue.trim();

	}

	public static HSSFCellStyle getDefaultHSSFCellStyle(HSSFWorkbook workbook,
			HSSFFont font) {
		// 设置内容单元格格式
		HSSFCellStyle cellStyle = workbook.createCellStyle();
		cellStyle.setFont(font);
		cellStyle.setAlignment(HSSFCellStyle.ALIGN_LEFT); // 水平布局：右对齐
		cellStyle.setWrapText(true);// 设置自动换行

		return cellStyle;
	}

	private static HSSFCellStyle getDefaultHSSFTitleStyle(
			HSSFWorkbook workbook, HSSFFont font) {
		// 设置title单元格格式
		HSSFCellStyle titleCellStyle = workbook.createCellStyle();
		titleCellStyle.setFont(font);
		titleCellStyle.setAlignment(HSSFCellStyle.ALIGN_LEFT); // 水平布局：右对齐
		titleCellStyle.setWrapText(true);// 设置自动换行
		// 设置黄色背景色
		titleCellStyle.setFillBackgroundColor(HSSFColor.YELLOW.index);
		titleCellStyle.setFillForegroundColor(HSSFColor.YELLOW.index);
		titleCellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);

		return titleCellStyle;
	}

	private static void output(HSSFWorkbook workbook, String fileName,
			HttpServletResponse response) {
		response.setContentType("application/vnd.ms-excel;charset=UTF-8");

		response.setHeader("Content-disposition", "attachment;filename="
				+ fileName + ".xls");
		OutputStream ouputStream = null;
		try {
			ouputStream = response.getOutputStream();
			workbook.write(ouputStream);
		} catch (IOException e) {
			logger.error("download order excel error:" + e);
		} finally {
			try {
				if (null != ouputStream) {
					ouputStream.flush();
				}
			} catch (IOException e) {
				logger.error("flush response ouputStream error:" + e);
			}
			try {
				if (null != ouputStream) {
					ouputStream.close();
				}
			} catch (IOException e) {
				logger.error("close response ouputStream error:" + e);
			}
		}
	}

}
