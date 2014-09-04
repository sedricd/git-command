repackage com.sfpay.sfgo_m.web.service.hessian.sfgo_m.impl;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sfpay.framework.base.exception.ServiceException;
import com.sfpay.framework.base.pagination.IPage;
import com.sfpay.sfgo.axg.domain.SfgoAxgOperateLog;
import com.sfpay.sfgo.axg.domain.SfgoLogisticsNoExt;
import com.sfpay.sfgo.axg.domain.SfgoOrder;
import com.sfpay.sfgo.axg.domain.SfgoOrderGoods;
import com.sfpay.sfgo.axg.domain.SfgoOrderPageResult;
import com.sfpay.sfgo.axg.domain.SfgoOrderRoute;
import com.sfpay.sfgo.common.util.DateUtil;
import com.sfpay.sfgo.common.util.MessageHelper;
import com.sfpay.sfgo.common.util.ReflectUtil;
import com.sfpay.sfgo.common.util.XssUtil;
import com.sfpay.sfgo_m.constants.Constants;
import com.sfpay.sfgo_m.domain.Address;
import com.sfpay.sfgo_m.domain.AxgOperateLog;
import com.sfpay.sfgo_m.domain.DisputeInfo;
import com.sfpay.sfgo_m.domain.LogisticsNoExt;
import com.sfpay.sfgo_m.domain.LogisticsOrder;
import com.sfpay.sfgo_m.domain.LogisticsOrderExt;
import com.sfpay.sfgo_m.domain.OrderGoods;
import com.sfpay.sfgo_m.domain.OrderRoute;
import com.sfpay.sfgo_m.domain.RefundInfo;
import com.sfpay.sfgo_m.domain.User;
import com.sfpay.sfgo_m.domain.vo.LogisticsOrderPrintInfo;
import com.sfpay.sfgo_m.domain.vo.RouteForm;
import com.sfpay.sfgo_m.enums.CommonParameter;
import com.sfpay.sfgo_m.enums.DataOperateType;
import com.sfpay.sfgo_m.enums.DisputeStatus;
import com.sfpay.sfgo_m.enums.OperateLogModel;
import com.sfpay.sfgo_m.enums.OperateLogType;
import com.sfpay.sfgo_m.enums.OperatorType;
import com.sfpay.sfgo_m.enums.OrderAxgStatusType;
import com.sfpay.sfgo_m.enums.OrderCreateType;
import com.sfpay.sfgo_m.enums.OrderPayType;
import com.sfpay.sfgo_m.enums.OrderServiceType;
import com.sfpay.sfgo_m.enums.SfExpressType;
import com.sfpay.sfgo_m.errorcode.CommonErrorCode;
import com.sfpay.sfgo_m.errorcode.LogisticsOrderErrorCode;
import com.sfpay.sfgo_m.errorcode.OrderGoodsErrorCode;
import com.sfpay.sfgo_m.skeleton.service.IAddressService;
import com.sfpay.sfgo_m.skeleton.service.IAppealInfoService;
import com.sfpay.sfgo_m.skeleton.service.IAxgOperateLogService;
import com.sfpay.sfgo_m.skeleton.service.IDisputeInfoService;
import com.sfpay.sfgo_m.skeleton.service.ILogisticsOrderService;
import com.sfpay.sfgo_m.skeleton.service.IOperateLogService;
import com.sfpay.sfgo_m.skeleton.service.IRefundInfoService;
import com.sfpay.sfgo_m.skeleton.service.ISystemConfigService;
import com.sfpay.sfgo_m.web.service.hessian.sfgo_m.ILogisticsOrderClient;
import com.sfpay.sfgo_m.web.util.AddressSplitUtil;
import com.sfpay.sfgo_m.web.util.AmountUtil;
import com.sfpay.sfgo_m.web.util.ExcelUtil;
import com.sfpay.sfgo_m.web.vo.LogisticsOrderAddForm;
import com.sfpay.sfgo_m.web.vo.LogisticsOrderBatchForm;
import com.sfpay.sfgo_m.web.vo.LogisticsOrderModifyForm;
import com.sfpay.sfgo_m.web.vo.OrderGoodsForm;
import com.sfpay.sfgo_m.web.vo.SfSendVo;
import com.spreada.utils.chinese.ZHConverter;

@Service("logisticsOrderClient")
public class LogisticsOrderClientImpl implements ILogisticsOrderClient {

	private static Logger logger = LoggerFactory.getLogger(LogisticsOrderClientImpl.class);

	@Autowired
	@Value("${import_excel_max_line}")
	private String maxLine;

	// 导出的临时文件夹
	@Autowired
	@Value("${export_tmp_dir}")
	private String exportPath;

	@Resource
	private ILogisticsOrderService logisticsOrderService;

	@Resource
	private IAddressService addressService;

	@Resource
	private ISystemConfigService systemConfigService;

	@Resource
	private IAppealInfoService appealInfoService;

	@Resource
	private IOperateLogService operateLogService;

	@Resource
	private IAxgOperateLogService axgOperateLogService;

	@Resource
	private IDisputeInfoService disputeInfoService;

	@Resource
	private IRefundInfoService refundInfoService;

	@Autowired
	private MessageHelper messageHelper;

	// 繁体订单导入模板的第一列列头
	@Autowired
	@Value("${traditional_temp_first_header}")
	private String traditional_header;

	@Override
	public void add(LogisticsOrderAddForm orderForm, User user, String createType) {

		LogisticsOrder order = new LogisticsOrder();

		initAddLogisticsOrderInfo(order, orderForm, user.getMemberNo(), createType, user);

		logisticsOrderService.add(order, user);

	}

	/**
	 * 初始化添加订单信息
	 * 
	 * @param order
	 * @param orderForm
	 * @param memberNo
	 * @param createType
	 */
	private void initAddLogisticsOrderInfo(LogisticsOrder order, LogisticsOrderAddForm orderForm, Long memberNo,
			String createType, User user) {

		LogisticsOrderExt orderExt = new LogisticsOrderExt();

		if (null != orderForm.getOrderExt()) {
			ReflectUtil.copyProperties(orderForm.getOrderExt(), orderExt);
		}

		ReflectUtil.copyProperties(orderForm, order);
		order.setMemberNo(memberNo);
		order.setSourceCreateDate(new Date());
		order.setCreateType(OrderCreateType.MANUAL_ADD.toString());
		if (StringUtils.isNotBlank(orderForm.getOrderAmount())) {
			if (!AmountUtil.isValidDigits(orderForm.getOrderAmount())) {
				throw new ServiceException(LogisticsOrderErrorCode.ORDER_AMOUNT_IS_INVALID);
			}
			order.setOrderAmount(new BigDecimal(orderForm.getOrderAmount()).multiply(new BigDecimal(100)).longValue());
		}
		// 设置是否保价
		if (StringUtils.isBlank(orderForm.getIsInsurance())) {
			order.setIsInsurance(CommonParameter.NO.toString());
		} else {
			order.setIsInsurance(orderForm.getIsInsurance());
		}
		if (StringUtils.isNotBlank(orderForm.getInsuranceAmount())) {
			if (!AmountUtil.isValidDigits(orderForm.getInsuranceAmount())) {
				throw new ServiceException(LogisticsOrderErrorCode.INSURANCE_AMOUNT_IS_INVALID);
			}
			order.setInsuranceAmount(new BigDecimal(orderForm.getInsuranceAmount()).multiply(new BigDecimal(100))
					.longValue());
		}
		if (StringUtils.isBlank(orderForm.getPayType())) {
			throw new ServiceException(LogisticsOrderErrorCode.ORDER_PAY_TYPE_IS_NULL);
		} else {
			if (OrderPayType.OTHER.toString().equals(orderForm.getPayType())) {
				order.setPayType(orderForm.getPayType());
				order.setServiceType(OrderServiceType.NORMAL.toString());
			} else if (OrderPayType.COD.toString().equals(orderForm.getPayType())) {
				order.setPayType(orderForm.getPayType());
				order.setServiceType(OrderServiceType.AXG.toString());
				if (StringUtils.isNotBlank(orderForm.getCodAmount())) {
					if (!AmountUtil.isValidDigits(orderForm.getCodAmount())) {
						throw new ServiceException(LogisticsOrderErrorCode.COD_AMOUNT_IS_INVALID);
					}
					order.setCodAmount(new BigDecimal(orderForm.getCodAmount()).multiply(new BigDecimal(100))
							.longValue());
				} else {
					throw new ServiceException(LogisticsOrderErrorCode.COD_AMOUNT_IS_INVALID);
				}
			} else {
				throw new ServiceException(LogisticsOrderErrorCode.ORDER_PAY_TYPE_IS_NULL);
			}
		}
		order.setPackWeight(orderForm.getPackWeight());
		order.setOrderExt(orderExt);
		// 设置物品信息
		initAddOrderGoodsInfo(order, orderForm.getGoods());
	}

	/**
	 * 设置新增订单物品信息
	 * 
	 * @param order
	 * @param goodsForm
	 */
	private void initAddOrderGoodsInfo(LogisticsOrder order, List<OrderGoodsForm> goodsForm) {
		List<OrderGoods> goodsList = null;
		if (null != goodsForm && goodsForm.size() > 0) {
			goodsList = new ArrayList<OrderGoods>(goodsForm.size());
			for (OrderGoodsForm form : goodsForm) {
				OrderGoods goods = new OrderGoods();
				ReflectUtil.copyProperties(form, goods);
				goods.setId(form.getId());
				if (StringUtils.isNotBlank(form.getPrice())) {
					if (!AmountUtil.isValidDigits(form.getPrice())) {
						throw new ServiceException(OrderGoodsErrorCode.GOODS_PRICE_IS_INVALID);
					}
					goods.setPrice(new BigDecimal(form.getPrice()).multiply(new BigDecimal(100)).longValue());
				}
				if (StringUtils.isNotBlank(form.getGoodsNum())) {
					if (!AmountUtil.isValidNumber(form.getGoodsNum())) {
						throw new ServiceException(OrderGoodsErrorCode.GOODS_QUANTITY_IS_INVALID);
					}
					goods.setGoodsNum(Integer.valueOf(form.getGoodsNum()));
				}
				goodsList.add(goods);
			}
		}
		order.setGoods(goodsList);
	}

	@Override
	public String importOrder(InputStream inputStream, String ext, boolean isAxg, User user) {

		StringBuilder result = new StringBuilder();
		int successNum = 0;
		StringBuilder sb = new StringBuilder();
		// 默认发货地址
		Address address = addressService.queryDefaultShipAddress(user.getMemberNo());

		String batchNo = systemConfigService.queryOrderBatchNoAndUpdateByMemberNo(user.getMemberNo());

		Map<Integer, ServiceException> errorOrders = new HashMap<Integer, ServiceException>();

		Map<Integer, LogisticsOrder> orders = readECSFileToOrder(inputStream, ext, address, isAxg, user, errorOrders);

		if (null != orders && orders.size() > 0) {
			for (Integer rowNum : orders.keySet()) {
				boolean isSuccess = false;
				LogisticsOrder order = orders.get(rowNum);
				try {
					// 校验订单数据
					validate4Import(order);

					// 货到付款设置为安心购
					if (order.isCod()) {
						order.setServiceType(OrderServiceType.AXG.toString());
					}

					LogisticsOrder oldOrder = logisticsOrderService.queryOrderBaseInfo(order.getOrderNo(),
							user.getMemberNo());

					// 判断是否已经导入了当前交易的其他物品,如果已经导入了订单，则只添加物品
					if (null != oldOrder) {
						treatImportOrderGoods(order, user);
					} else {
						// 新增的订单设置批次号
						order.setBatchNo(batchNo);
						isSuccess = logisticsOrderService.add(order, user);
					}
					if (isSuccess) {
						successNum++;
					}
				} catch (ServiceException e) {
					logger.error("添加导入订单失败：", e);
					String msg = "";
					if (StringUtils.isNotBlank(e.getCode())) {
						String[] codes = e.getCode().split("#");
						msg = messageHelper.getMessage(codes[0]);
						if (codes.length > 1) {
							msg = msg.replace("{0}", codes[1]);
						}
					} else {
						msg = e.getMsg();
					}
					sb.append(rowNum).append("(").append(msg).append(")").append(Constants.COMMA);
				} catch (Exception e) {
					logger.error("添加导入订单失败：", e);
					sb.append(rowNum).append(Constants.COMMA);
				}
			}
		}
		result.append("成功导入");
		result.append(successNum).append("行");
		for (Integer rowNum : errorOrders.keySet()) {
			sb.append(rowNum).append("(")
					.append(messageHelper.getMessage(errorOrders.get(rowNum).getCode(), Locale.SIMPLIFIED_CHINESE))
					.append(")").append(Constants.COMMA);
		}
		if (StringUtils.isNotBlank(sb.toString())) {
			result.append("，第").append(sb.toString().substring(0, sb.lastIndexOf(Constants.COMMA))).append("行导入失败。");
		} else {
			result.append("。");
		}
		return result.toString().replaceAll("。", StringUtils.EMPTY);

	}

	/**
	 * 方法说明：导入时校验订单<br>
	 * 
	 * @param order
	 */
	private void validate4Import(LogisticsOrder order) {
		// 电话和手机不能同时为空
		String receiverMobile = order.getReceiverMobile();
		String receiverPhone = order.getReceiverPhone();
		if (StringUtils.isBlank(receiverMobile) && StringUtils.isBlank(receiverPhone)) {
			throw new ServiceException(LogisticsOrderErrorCode.MOBILE_AND_PHONE_IS_NULL);
		}
		if (!validatePhone(receiverMobile)) {
			throw new ServiceException(LogisticsOrderErrorCode.RECEIVER_MOBILE_IS_INVALID);
		}
		if (!validatePhone(receiverPhone)) {
			throw new ServiceException(LogisticsOrderErrorCode.RECEIVER_PHONE_IS_INVALID);
		}

		// 买家昵称不能大于15个字
		String buyerNick = order.getBuyerNick();
		if (StringUtils.isNotBlank(buyerNick) && buyerNick.length() > 15) {
			throw new ServiceException(LogisticsOrderErrorCode.BUYER_NICK_IS_GREATER_THAN_15);
		}
	}

	/**
	 * 如果有相同物品则修改，无则新增
	 * 
	 * @param order
	 * @param user
	 *            已导入的交易信息
	 * @param user
	 */
	private void treatImportOrderGoods(LogisticsOrder order, User user) {
		for (OrderGoods orderGoods : order.getGoods()) {
			orderGoods.setOrderNo(order.getOrderNo());
			logisticsOrderService.treatImportOrderGoods(orderGoods, user);
		}
	}

	private Map<Integer, LogisticsOrder> readECSFileToOrder(InputStream is, String ext, Address shipperAddress,
			boolean isAxg, User user, Map<Integer, ServiceException> errorOrders) throws ServiceException {

		if (("xls").equals(ext)) {
			return readECSXls(is, shipperAddress, isAxg, user, errorOrders);
		} else if (("xlsx").equals(ext)) {
			return readECSXlsx(is, shipperAddress, isAxg, user, errorOrders);
		} else {
			throw new ServiceException(CommonErrorCode.IMPORT_FILE_DATA_INVALID);
		}

	}

	/**
	 * 读取ECS excel模板文件
	 * 
	 * @param is
	 * @param shipperAddress
	 * @param isAxg
	 * @param user
	 * @return
	 * @throws ServiceException
	 */
	private Map<Integer, LogisticsOrder> readECSXls(InputStream is, Address shipperAddress, boolean isAxg, User user,
			Map<Integer, ServiceException> errorOrders) throws ServiceException {
		POIFSFileSystem fs;
		HSSFWorkbook wb;
		HSSFSheet sheet;
		HSSFRow row;

		TreeMap<Integer, LogisticsOrder> orders = new TreeMap<Integer, LogisticsOrder>();
		// 导入订单序号与订单号对应关系
		// ECS模板通过订单序号来判断多行数据是否属于同一个订单，第一次读取某个序号时，生成该序号对应的订单号
		Map<String, String> orderNoMap = new HashMap<String, String>();

		try {
			// excel表格有效列数
			int minColumNum = 21;
			fs = new POIFSFileSystem(is);
			wb = new HSSFWorkbook(fs);

			sheet = wb.getSheetAt(0);
			// 得到总行数
			int totalRowNum = 0;
			try {
				HSSFRow totalNumAtRow = wb.getSheetAt(1).getRow(5);
				String totalRowNumStr = ExcelUtil.getHSSFCellValue(totalNumAtRow, 1);
				totalRowNum = Integer.parseInt(totalRowNumStr);
			} catch (Exception e) {
				totalRowNum = sheet.getLastRowNum();
			}

			if (totalRowNum > Integer.parseInt(maxLine)) {
				throw new ServiceException(LogisticsOrderErrorCode.IMPORT_EXCEL_GREATER_THAN_MAX_LINE);
			}
			row = sheet.getRow(0);
			int colNum = row.getPhysicalNumberOfCells();
			if (colNum != minColumNum) {
				throw new ServiceException(CommonErrorCode.IMPORT_FILE_IS_NOT_NEWER);
			}
			// 通过第一列列头判断是否为繁体模板，add by 349474 since 1.3.0
			boolean isTraditional = traditional_header.equals(ExcelUtil.getHSSFCellValue(row, 0));

			// 正文内容应该从第二行开始,第一行为表头的标题
			for (int rowNum = 2; rowNum <= totalRowNum + 1; rowNum++) {
				LogisticsOrder order = new LogisticsOrder();
				LogisticsOrderExt orderExt = new LogisticsOrderExt();
				order.setOrderExt(orderExt);
				List<OrderGoods> goods = new ArrayList<OrderGoods>();

				OrderGoods orderGoods = new OrderGoods();
				row = sheet.getRow(rowNum);
				int j = 0;
				int blackCell = 0;
				try {
					while (j < colNum) {
						blackCell = setECSDataToOrder(j, ExcelUtil.getHSSFCellValue(row, j), order, orderExt,
								orderGoods, isAxg, blackCell, isTraditional);
						j++;
					}
				} catch (ServiceException e) {
					errorOrders.put(rowNum, e);
					continue;
				}
				// 如果是空白行，跳过
				if (blackCell == minColumNum) {
					continue;
				}

				initECSOrderInfo(orderNoMap, order, orderExt, orderGoods, isAxg);

				initImportBaseInfo(order, orderExt, orderGoods, user, shipperAddress);

				goods.add(orderGoods);
				order.setGoods(goods);

				orders.put(rowNum + 1, order);
			}

			orderNoMap.clear();
		} catch (ServiceException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("读取ECS excel文件失败：", e);
			throw new ServiceException(LogisticsOrderErrorCode.READ_IMPORT_FILE_FAIL);
		} finally {
			if (null != is) {
				try {
					is.close();
				} catch (Exception e1) {
					logger.error("关闭导入订单文件流失败：", e1);
				}
			}
		}
		return orders.descendingMap();
	}

	private void initECSOrderInfo(Map<String, String> orderNoMap, LogisticsOrder order, LogisticsOrderExt orderExt,
			OrderGoods orderGoods, boolean isAxg) {
		// 生成订单号
		String orderNo = null;
		if (StringUtils.isBlank(orderNoMap.get(order.getOrderNo()))) {
			orderNo = logisticsOrderService.queryOrderNo();
			// 写入订单序号与订单号对应关系
			orderNoMap.put(order.getOrderNo(), orderNo);
		} else {
			orderNo = orderNoMap.get(order.getOrderNo());
		}
		order.setOrderNo(orderNo);

		// TODO ORDER_AMOUNT
		if (null != order.getCodAmount() && order.getCodAmount() > Constants.LONG_ZERO) {
			if (isAxg) {
				order.setServiceType(OrderServiceType.AXG.toString());
			} else {
				order.setServiceType(OrderServiceType.COD.toString());
			}
		} else {
			order.setServiceType(OrderServiceType.NORMAL.toString());
		}
		order.setSourceCreateDate(new Date());
	}

	private void initImportBaseInfo(LogisticsOrder order, LogisticsOrderExt orderExt, OrderGoods orderGoods, User user,
			Address shipperAddress) {
		orderGoods.setMemberNo(user.getMemberNo());
		// 设置交易信息
		order.setCreateType(OrderCreateType.IMPORT.toString());

		// 设置订单信息
		order.setMemberNo(user.getMemberNo());
		if (null == order.getCodAmount() || order.getCodAmount() <= 0l) {
			order.setPayType(OrderPayType.OTHER.toString());
		} else {
			order.setPayType(OrderPayType.COD.getKey());
		}

		if (null != order.getInsuranceAmount() && order.getInsuranceAmount() > Constants.LONG_ZERO) {
			order.setIsInsurance(CommonParameter.YES.toString());
		} else {
			order.setIsInsurance(CommonParameter.NO.toString());
		}
		// 设置发货地址
		initShipperInfo(order, orderExt, shipperAddress);
	}

	/**
	 * 读取ECS excel模板文件
	 * 
	 * @param is
	 * @param shipperAddress
	 * @param isAxg
	 * @param user
	 * @return
	 * @throws ServiceException
	 */
	private Map<Integer, LogisticsOrder> readECSXlsx(InputStream is, Address shipperAddress, boolean isAxg, User user,
			Map<Integer, ServiceException> errorOrders) throws ServiceException {
		XSSFWorkbook wb;
		XSSFSheet sheet;
		XSSFRow row;

		Map<Integer, LogisticsOrder> orders = new HashMap<Integer, LogisticsOrder>();

		Map<String, String> orderNoMap = new HashMap<String, String>();

		try {
			int minColumNum = 21;
			wb = new XSSFWorkbook(is);

			sheet = wb.getSheetAt(0);
			// 得到总行数
			int totalRowNum = sheet.getLastRowNum();
			if (totalRowNum > Integer.parseInt(maxLine)) {
				throw new ServiceException(LogisticsOrderErrorCode.IMPORT_EXCEL_GREATER_THAN_MAX_LINE);
			}
			row = sheet.getRow(0);
			int colNum = row.getPhysicalNumberOfCells();
			if (colNum < minColumNum) {
				throw new ServiceException(CommonErrorCode.IMPORT_FILE_DATA_INVALID);
			}
			// 通过第一列列头判断是否为繁体模板，add by 349474 since 1.3.0
			// boolean isTraditional =
			// traditional_header.equals(ExcelUtil.getXSSFCellValue(row, 0));

			// 正文内容应该从第二行开始,第一行为表头的标题
			for (int rowNum = 1; rowNum <= totalRowNum; rowNum++) {
				LogisticsOrder order = new LogisticsOrder();
				LogisticsOrderExt orderExt = new LogisticsOrderExt();
				order.setOrderExt(orderExt);
				List<OrderGoods> goods = new ArrayList<OrderGoods>();
				OrderGoods orderGoods = new OrderGoods();
				row = sheet.getRow(rowNum);
				int j = 0;
				int blackCell = 0;
				try {
					while (j < colNum) {
						// 设置值
						blackCell = setECSDataToOrder(j, ExcelUtil.getXSSFCellValue(row, j), order, orderExt,
								orderGoods, isAxg, blackCell, false);
						j++;
					}
				} catch (ServiceException e) {
					errorOrders.put(rowNum, e);
					continue;
				}
				if (blackCell == minColumNum) {
					continue;
				}

				// 生成订单号
				String orderNo = null;
				if (StringUtils.isBlank(orderNoMap.get(order.getOrderNo()))) {
					orderNo = logisticsOrderService.queryOrderNo();
					// 写入订单序号与订单号对应关系
					orderNoMap.put(order.getOrderNo(), orderNo);
				} else {
					orderNo = orderNoMap.get(order.getOrderNo());
				}
				order.setOrderNo(orderNo);

				order.setSourceCreateDate(new Date());

				initImportBaseInfo(order, orderExt, orderGoods, user, shipperAddress);

				goods.add(orderGoods);
				order.setGoods(goods);

				orders.put(rowNum, order);
			}

			orderNoMap.clear();
		} catch (ServiceException e) {
			throw e;
		} catch (Exception e) {
			logger.error("读取ECS excel文件失败：", e);
			throw new ServiceException(LogisticsOrderErrorCode.READ_IMPORT_FILE_FAIL);
		} finally {
			if (null != is) {
				try {
					is.close();
				} catch (Exception e1) {
					logger.error("关闭导入订单文件流失败：", e1);
				}
			}
		}
		return orders;
	}

	/**
	 * ECS模板文件数据转换为对象值
	 * 
	 * @param column
	 * @param data
	 * @param order
	 * @param orderExt
	 * @param orderGoods
	 * 
	 * @return 单元格数据是否不存在
	 */
	private int setECSDataToOrder(int column, String data, LogisticsOrder order, LogisticsOrderExt orderExt,
			OrderGoods orderGoods, boolean isAxg, int blackCellNum, boolean isTraditional) {
		data = XssUtil.xssEncode(data);
		if (StringUtils.isBlank(data)) {
			blackCellNum += 1;
		}
		if (column == 0) {
			if (StringUtils.isBlank(data)) {
				throw new ServiceException(LogisticsOrderErrorCode.IMPORT_ORDER_NO_IS_NULL);
			}
			order.setOrderNo(data);
		} else if (column == 1) {
			// 联系人
			order.setReceiverName(data);
		} else if (column == 2) {
			// 联系电话
			order.setReceiverPhone(data);
		} else if (column == 3) {
			// 联系手机
			order.setReceiverMobile(data);
		} else if (column == 4) {
			// 繁体转简体
			if (isTraditional) {
				data = ZHConverter.convert(data, ZHConverter.SIMPLIFIED);
			}
			// 收件详细地址
			order.setReceiverAddress(data);

			// 拆分地址
			AddressSplitUtil.splitOrderReceiverAddress(order, data);
		} else if (column == 5) {
			// 买家昵称
			order.setBuyerNick(data);
		} else if (column == 6) {
			// 商品名称
			orderGoods.setGoodsName(StringUtils.isNotBlank(data) ? data : null);
		} else if (column == 7) {
			if (StringUtils.isBlank(data)) {
				throw new ServiceException(OrderGoodsErrorCode.GOODS_NUM_IS_INVALID);
			}
			// 商品数量
			try {
				if (StringUtils.isNotBlank(StringUtils.trim(data))) {
					orderGoods.setGoodsNum(Integer.valueOf(StringUtils.trim(data)));
				}
			} catch (Exception e) {
				throw new ServiceException(OrderGoodsErrorCode.GOODS_NUM_IS_INVALID);
			}
		} else if (column == 8) {
			// 商品重量
			order.setPackWeight(data);
		} else if (column == 9) {
			// 订单金额
			if (StringUtils.isNotBlank(StringUtils.trim(data))) {
				try {
					order.setOrderAmount(new BigDecimal(StringUtils.trim(data)).multiply(new BigDecimal(100))
							.longValue());
				} catch (Exception e) {
					logger.error("导入订单转换订单额失败：", e);
					throw new ServiceException(LogisticsOrderErrorCode.ORDER_AMOUNT_IS_INVALID);
				}
			} else {
				order.setOrderAmount(0L);
			}
		} else if (column == 10) {
			// 商品金额
			if (StringUtils.isNotBlank(StringUtils.trim(data))) {
				try {
					orderGoods.setPrice(new BigDecimal(StringUtils.trim(data)).multiply(new BigDecimal(100))
							.longValue());
				} catch (Exception e) {
					logger.error("导入订单转换商品金额失败：", e);
					throw new ServiceException(OrderGoodsErrorCode.GOODS_PRICE_IS_INVALID);
				}
			} else {
				orderGoods.setPrice(0L);
			}
		} else if (column == 11) {
			// 代收金额
			if (isAxg && StringUtils.isBlank(data)) {
				throw new ServiceException(LogisticsOrderErrorCode.AXG_ORDER_COD_AMOUNT_MUST_BE_GREATER_THAN_ZERO);
			}
			if (StringUtils.isNotBlank(StringUtils.trim(data))) {
				try {
					order.setCodAmount(new BigDecimal(StringUtils.trim(data)).multiply(new BigDecimal(100)).longValue());
				} catch (Exception e) {
					logger.error("导入订单转换代收货款金额失败：", e);
					throw new ServiceException(LogisticsOrderErrorCode.COD_AMOUNT_IS_INVALID);
				}
			}
		} else if (column == 12) {
			// 保价金额
			if (StringUtils.isNotBlank(StringUtils.trim(data))) {
				try {
					order.setInsuranceAmount(new BigDecimal(StringUtils.trim(data)).multiply(new BigDecimal(100))
							.longValue());
				} catch (Exception e) {
					logger.error("导入订单转换保价货款金额失败：", e);
					throw new ServiceException(LogisticsOrderErrorCode.INSURANCE_AMOUNT_IS_INVALID);
				}
			}
		} else if (column == 13) {
			// 纸箱费
			if (StringUtils.isNotBlank(StringUtils.trim(data))) {
				try {
					order.setPackAmount(new BigDecimal(StringUtils.trim(data)).multiply(new BigDecimal(100))
							.longValue());
				} catch (Exception e) {
					logger.error("导入包装费失败：", e);
					throw new ServiceException(LogisticsOrderErrorCode.PACKAGE_AMOUNT_IS_INVALID);
				}
			}
		} else if (column == 14) {
			if (StringUtils.isNotBlank(data)) {
				order.setExpressType(SfExpressType.getKey(data));
			}
		} else if (column == 15) {
			if (StringUtils.isNotBlank(data)) {
				try {
					order.getOrderExt().setExpressNum(Integer.parseInt(data));
				} catch (NumberFormatException e) {
					throw new ServiceException(LogisticsOrderErrorCode.ERXPRESS_NUM_IS_INVALID);
				}
			} else {
				order.getOrderExt().setExpressNum(1);
			}
		} else if (column == 16) {
			// 买家留言
			order.setReceiverMessage(data);
		} else if (column == 17) {
			// 卖家留言
			order.setShipperMessage(data);
		} else if (column == 18) {
			order.setOrderExt1(data);
		} else if (column == 19) {
			order.setOrderExt2(data);
		} else if (column == 20) {
			order.setOrderExt3(data);
		}

		return blackCellNum;
	}

	/**
	 * 
	 * 方法说明：校验手机或电话号码为数字且长度小于20位<br>
	 * 
	 * @param phoneNo
	 * @return
	 * @author 349474
	 * @since 1.3.0
	 */
	private boolean validatePhone(String phoneNo) {
		if (StringUtils.isBlank(phoneNo)) {
			return true;
		}

		String phone = phoneNo.replace("-", "");
		return StringUtils.isNumeric(phone) && phone.length() <= 20;
	}

	private void initShipperInfo(LogisticsOrder order, LogisticsOrderExt orderExt, Address shipperAddress) {
		order.setShipperName(shipperAddress.getContact());
		order.setShipperProvince(shipperAddress.getProvince());
		order.setShipperCity(shipperAddress.getCity());
		order.setShipperCounty(shipperAddress.getDistrict());
		order.setShipperAddress(shipperAddress.getAddress());
		order.setShipperMobile(shipperAddress.getMobile());
		order.setShipperPhone(shipperAddress.getPhone());
		orderExt.setShipperAddressCode(shipperAddress.getCityCode());
		orderExt.setShipperExt1(shipperAddress.getExt1());
		orderExt.setShipperExt2(shipperAddress.getExt2());
		orderExt.setShipperExt3(shipperAddress.getExt3());
	}

	@Override
	public void modify(LogisticsOrderModifyForm orderForm, User user) {

		LogisticsOrder order = new LogisticsOrder();

		initModifyLogisticsOrderInfo(order, orderForm, user.getMemberNo());

		logisticsOrderService.modify(order, user);
	}

	/**
	 * 初始化修改订单信息
	 * 
	 * @param order
	 * @param orderForm
	 * @param memberNo
	 */
	private void initModifyLogisticsOrderInfo(LogisticsOrder order, LogisticsOrderModifyForm orderForm, Long memberNo) {

		ReflectUtil.copyProperties(orderForm, order);

		LogisticsOrderExt orderExt = new LogisticsOrderExt();
		if (null != orderForm.getOrderExt()) {
			ReflectUtil.copyProperties(orderForm.getOrderExt(), orderExt);
		}

		if (null != orderForm.getOrderAmount()) {
			if (!AmountUtil.isValidDigits(orderForm.getOrderAmount())) {
				throw new ServiceException(LogisticsOrderErrorCode.ORDER_AMOUNT_IS_INVALID);
			}
			order.setOrderAmount(new BigDecimal(orderForm.getOrderAmount()).multiply(new BigDecimal(100)).longValue());
		} else {
			order.setOrderAmount(Constants.LONG_ZERO);
		}

		if (StringUtils.isNotBlank(orderForm.getIsInsurance())) {
			order.setIsInsurance(orderForm.getIsInsurance());
		}
		if (StringUtils.isNotBlank(orderForm.getInsuranceAmount())) {
			if (!AmountUtil.isValidDigits(orderForm.getInsuranceAmount())) {
				throw new ServiceException(LogisticsOrderErrorCode.INSURANCE_AMOUNT_IS_INVALID);
			}
			order.setInsuranceAmount(new BigDecimal(orderForm.getInsuranceAmount()).multiply(new BigDecimal(100))
					.longValue());
		} else {
			order.setInsuranceAmount(Constants.LONG_ZERO);
		}
		if (StringUtils.isNotBlank(orderForm.getCodAmount())) {
			if (!AmountUtil.isValidDigits(orderForm.getCodAmount())) {
				throw new ServiceException(LogisticsOrderErrorCode.COD_AMOUNT_IS_INVALID);
			}
			order.setCodAmount(new BigDecimal(orderForm.getCodAmount()).multiply(new BigDecimal(100)).longValue());
			// if (orderForm.isAxg()) {
			// order.setServiceType(OrderServiceType.AXG.toString());
			// } else {
			// order.setServiceType(OrderServiceType.COD.toString());
			// }
		} else {
			order.setCodAmount(Constants.LONG_ZERO);
		}

		if (StringUtils.isNotBlank(orderForm.getPackAmount())) {
			if (!AmountUtil.isValidDigits(orderForm.getPackAmount())) {
				throw new ServiceException(LogisticsOrderErrorCode.PACKAGE_AMOUNT_IS_INVALID);
			}
			order.setPackAmount(new BigDecimal(orderForm.getPackAmount()).multiply(new BigDecimal(100)).longValue());
		}
		// 设置删除物品ID
		order.setDeleteIds(orderForm.getDeleteIds());
		// 设置物品信息
		initAddOrderGoodsInfo(order, orderForm.getGoods());

		order.setOrderExt(orderExt);
	}

	@Override
	public IPage<LogisticsOrder> queryOrderPageList(Map<String, Object> paramMap, Integer pageNo, Integer pageSize) {
		return logisticsOrderService.queryOrderPageList(paramMap, pageNo, pageSize);
	}

	@Override
	public Map<String, String> check(List<String> orderNoList, User user, Map<String, String> orderTypePayCustIdMap) {
		return logisticsOrderService.check(orderNoList, user, orderTypePayCustIdMap);
	}

	@Override
	public LogisticsOrder queryOrderInfo(String orderNo, Long memberNo) {
		return logisticsOrderService.queryOrderInfo(orderNo, memberNo);
	}

	@Override
	public Map<String, String> consign(List<String> orderNoList, User user) {
		return logisticsOrderService.consign(orderNoList, user);
	}

	@Override
	public String receiveRoute(String routeMsg) {
		return logisticsOrderService.receiveRoute(routeMsg);
	}

	@Override
	public String mockRoute(RouteForm form) {
		return logisticsOrderService.mockRoute(form);
	}

	@Override
	public List<OrderRoute> queryOrderRoute(String orderNo, Long memberNo) {
		return logisticsOrderService.queryOrderRoute(orderNo, memberNo);
	}

	@Override
	public List<LogisticsOrderPrintInfo> queryPrintWaybillOrderList(List<String> orderNoList, User user, boolean isPrint) {
		return logisticsOrderService.queryPrintWaybillOrderList(orderNoList, user, isPrint);
	}

	@Override
	public List<LogisticsOrder> queryPrintOrderList(List<String> orderNo, User user) {
		return logisticsOrderService.queryPrintOrderList(orderNo, user);
	}

	@Override
	public Integer countOrder(Long memberNo, String orderStatus) {
		return logisticsOrderService.countOrder(memberNo, orderStatus);
	}

	@Override
	public Integer countSendOrder(Map<String, Object> paramMap) {
		return logisticsOrderService.countSendOrder(paramMap);
	}

	@Override
	public Integer countAxgOrder(Long memberNo, String axgStatus) {
		return logisticsOrderService.countAxgOrder(memberNo, axgStatus);
	}

	@Override
	public List<OrderGoods> queryPrintGoodsbill(List<String> orderNoList, User user) {
		return logisticsOrderService.queryPrintGoodsbill(orderNoList, user);
	}

	@Override
	public Map<String, String> cancelDeliver(List<String> orderNos, User user) {
		return logisticsOrderService.cancelDeliver(orderNos, user);
	}

	@Override
	public void markOrder(List<String> orderNos, String markLevel, User user) {
		logisticsOrderService.markLevel(orderNos, markLevel, user);
	}

	@Override
	public Map<String, String> modifyByOrderNoList(List<String> orderNos, LogisticsOrderBatchForm orderForm, User user) {
		LogisticsOrder order = new LogisticsOrder();
		LogisticsOrderExt orderExt = new LogisticsOrderExt();
		// 批量修改场景Null的参数表示不修改
		ReflectUtil.copyProperties(orderForm, order);
		if (null != orderForm.getOrderExt()) {
			if (null != orderForm.getOrderExt().getExpressNum()) {
				orderExt.setExpressNum(orderForm.getOrderExt().getExpressNum());
				order.setOrderExt(orderExt);
			}
			if (StringUtils.isNotBlank(orderForm.getOrderExt().getLogisticsReturnFlag())) {
				orderExt.setLogisticsReturnFlag(orderForm.getOrderExt().getLogisticsReturnFlag());
				order.setOrderExt(orderExt);
			}
		}

		if (StringUtils.isNotBlank(orderForm.getInsuranceAmount())) {
			if (!AmountUtil.isValidDigits(orderForm.getInsuranceAmount())) {
				throw new ServiceException(LogisticsOrderErrorCode.INSURANCE_AMOUNT_IS_INVALID);
			}
			order.setInsuranceAmount(new BigDecimal(orderForm.getInsuranceAmount()).multiply(new BigDecimal(100))
					.longValue());
		}
		if (StringUtils.isNotBlank(orderForm.getPackAmount())) {
			if (!AmountUtil.isValidDigits(orderForm.getPackAmount())) {
				throw new ServiceException(LogisticsOrderErrorCode.PACK_AMOUNT_IS_INVALID);
			}
			order.setPackAmount(new BigDecimal(orderForm.getPackAmount()).multiply(new BigDecimal(100)).longValue());
		}
		return logisticsOrderService.modifyByOrderNoList(orderNos, order, user);
	}

	@Override
	public void recycleOrder(List<String> orderNos, User user) {
		logisticsOrderService.recycleOrder(orderNos, user);
	}

	@Override
	public void deleteOrder(List<String> orderNos, User user) {
		logisticsOrderService.deleteOrder(orderNos, user);
	}

	@Override
	public void resumeOrder(List<String> orderNos, User user) {
		logisticsOrderService.resumeOrder(orderNos, user);
	}

	@Override
	public void createAppeal(String orderNo, String appealType, String reason, User user) {
		appealInfoService.createAppeal(orderNo, appealType, reason, user);
	}

	@Override
	public void exportOrder(Map<String, Object> paramMap, User user, HttpServletResponse response) {

		String tepFileDirRealPath = exportPath;
		String memberDirPath = String.valueOf(user.getMemberNo());
		String exportDirPath = tepFileDirRealPath + File.separator + memberDirPath;
		File exportDir = new File(exportDirPath);
		if (!exportDir.exists()) {
			exportDir.mkdirs();
		}
		String exportFileName = exportDir + File.separator + DateUtil.date2Str(new Date(), "yyyyMMddHHmm") + ".csv";

		int currPage = 0;
		long totalPage = 0;
		long total = 0;

		try {
			do {
				IPage<LogisticsOrder> orderList = queryOrderPageList(paramMap, currPage,
						Constants.DEFAULT_EXPORT_PAGE_SIZE);
				totalPage = orderList.getTotal();
				total = orderList.getTotalRecord();
				currPage++;
				ExcelUtil.generateLogisticsOrderCsv((List<LogisticsOrder>) orderList.getData(), exportFileName,
						response);
			} while (currPage < totalPage);
		} catch (Exception e) {
			logger.error("导入订单失败：", e);
			throw new ServiceException(LogisticsOrderErrorCode.EXPORT_FAIL);
		}

		// 添加日志
		operateLogService.addOperateLog(messageHelper.getMessage("export_order_log", String.valueOf(total)),
				OperateLogType.SYSTEM_LOG, OperateLogModel.ORDER_MANAGE, DataOperateType.QUERY,
				OperatorType.MERCHANT.getCode(), Constants.DEFAULT_OPERATOR_ID, user);

		exportDir.delete();

	}

	@Override
	public List<AxgOperateLog> queryAxgOperateLog(String orderNo) {
		return axgOperateLogService.query(orderNo);
	}

	@Override
	public Map<String, String> countGroupOrders(Long memberNo) {
		return logisticsOrderService.countGroupOrdesr(memberNo);
	}

	@Override
	public void saveEcsAxgOrder(SfgoOrderPageResult orderPageResult, User user) {
		if (null != orderPageResult.getOrders() && orderPageResult.getOrders().size() > 0) {
			for (SfgoOrder sfgoOrder : orderPageResult.getOrders()) {

				try {
					// 设置订单主体
					LogisticsOrder order = new LogisticsOrder();
					ReflectUtil.copyProperties(sfgoOrder, order);
					order.setMemberNo(user.getMemberNo());
					order.setServiceType(OrderServiceType.AXG.toString());
					order.setPayType(OrderPayType.COD.toString());
					// 重新设置发货方式
					order.setTemplateId(null);
					if (!OrderAxgStatusType.PROTECTING.toString().equals(sfgoOrder.getAxgStatus())) {
						order.setProtectEndDate(null);
					}

					// 设置订单商品
					List<OrderGoods> orderGoodsList = new ArrayList<OrderGoods>();
					for (SfgoOrderGoods sfgoOrderGoods : sfgoOrder.getOrderGoodsList()) {
						OrderGoods orderGoods = new OrderGoods();
						ReflectUtil.copyProperties(sfgoOrderGoods, orderGoods);
						orderGoods.setMemberNo(user.getMemberNo());
						orderGoodsList.add(orderGoods);
					}
					order.setGoods(orderGoodsList);
					order.setCreateType(OrderCreateType.ECS_CONVERT.toString());

					// 设置订单扩展信息
					LogisticsOrderExt orderExt = new LogisticsOrderExt();
					ReflectUtil.copyProperties(sfgoOrder.getLogisticsOrderExt(), orderExt);
					order.setOrderExt(orderExt);

					// 保存订单信息
					logisticsOrderService.add(order, user);

					// 设置运单扩展信息

					if (null != sfgoOrder.getLogisticsNoExtList() && sfgoOrder.getLogisticsNoExtList().size() > 0) {
						List<LogisticsNoExt> logisticsNoExtList = new ArrayList<LogisticsNoExt>();
						for (SfgoLogisticsNoExt sfgoLogisticsNoExt : sfgoOrder.getLogisticsNoExtList()) {

							LogisticsNoExt logisticsNoExt = new LogisticsNoExt();
							ReflectUtil.copyProperties(sfgoLogisticsNoExt, logisticsNoExt);
							logisticsNoExt.setMemberNo(user.getMemberNo());
						}
						// 保存运单扩展信息
						logisticsOrderService.addLogisticsNoExt(logisticsNoExtList);
					}

					// 设置安心购操作日志

					if (null != sfgoOrder.getAxgOperateLogList() && sfgoOrder.getAxgOperateLogList().size() > 0) {
						List<AxgOperateLog> axgOperateLogList = new ArrayList<AxgOperateLog>();
						for (SfgoAxgOperateLog axgLog : sfgoOrder.getAxgOperateLogList()) {
							AxgOperateLog axgOperateLog = new AxgOperateLog();
							ReflectUtil.copyProperties(axgLog, axgOperateLog);
							axgOperateLogList.add(axgOperateLog);
						}
						logisticsOrderService.addAxgOperateLog(axgOperateLogList);
					}

					// 保存纠纷信息
					if (null != sfgoOrder.getDisputeInfo()) {
						DisputeInfo disputeInfo = new DisputeInfo();
						ReflectUtil.copyProperties(sfgoOrder.getDisputeInfo(), disputeInfo);
						disputeInfo.setMemberNo(user.getMemberNo());
						disputeInfo.setDisputeSolveEndDate(DateUtil.getDateOfDay(
								DateUtil.ceilDate(sfgoOrder.getDisputeInfo().getCreateDate()), 7));
						disputeInfo.setDisputeAmount(sfgoOrder.getCodAmount());
						if (OrderAxgStatusType.REFUNDING.toString().equals(sfgoOrder.getAxgStatus())) {
							disputeInfo.setStatus(DisputeStatus.REFUND.toString());
						}
						disputeInfoService.addDisputeInfo(disputeInfo);
					}

					// 保存退款信息
					if (null != sfgoOrder.getRefundInfo()) {
						RefundInfo refundInfo = new RefundInfo();
						ReflectUtil.copyProperties(sfgoOrder.getRefundInfo(), refundInfo);
						refundInfo.setMemberNo(user.getMemberNo());
						refundInfoService.addRefundInfo(refundInfo);
					}
					// 保存物流
					if (null != sfgoOrder.getRouteList() && sfgoOrder.getRouteList().size() > 0) {
						List<OrderRoute> routeList = new ArrayList<OrderRoute>();
						for (SfgoOrderRoute sfgoRoute : sfgoOrder.getRouteList()) {
							OrderRoute orderRoute = new OrderRoute();
							ReflectUtil.copyProperties(sfgoRoute, orderRoute);
							routeList.add(orderRoute);
						}
						logisticsOrderService.addOrderRoute(routeList);
					}
				} catch (Exception e) {
					logger.error("保存速运通安心购订单失败：", e);
				}
			}

		}

	}

	@Override
	public void exportSfOrder(List<SfSendVo> orders, HttpServletResponse response, User user, String expressStatus) {
		String tepFileDirRealPath = exportPath;
		String memberDirPath = String.valueOf(user.getMemberNo());
		String exportDirPath = tepFileDirRealPath + File.separator + memberDirPath;
		File exportDir = new File(exportDirPath);
		if (!exportDir.exists()) {
			exportDir.mkdirs();
		}
		String[] tagNameArr = new String[] { "", "全部", "已签收", "未签收" };
		String tagName = tagNameArr[Integer.parseInt(expressStatus)];
		String fileName = "SF快件_" + tagName + "_" + DateUtil.date2Str(new Date(), "yyyyMMdd") + ".csv";
		String exportFileName = exportDir + File.separator + fileName;
		ExcelUtil.exportSfOrder(orders, response, exportFileName, expressStatus);

		long total = 0L;
		if (orders != null) {
			total = orders.size();
		}
		// 添加日志
		operateLogService.addOperateLog(messageHelper.getMessage("export_order_log", String.valueOf(total)),
				OperateLogType.SYSTEM_LOG, OperateLogModel.ORDER_MANAGE, DataOperateType.QUERY,
				OperatorType.MERCHANT.getCode(), Constants.DEFAULT_OPERATOR_ID, user);
	}

	@Override
	public void ecsOrderConvert(SfgoOrder sfgoOrder, User user) {
		LogisticsOrder order = new LogisticsOrder();
		ReflectUtil.copyProperties(sfgoOrder, order);
		// 重新生成订单号
		order.setOrderNo(null);
		order.setCreateType(OrderCreateType.ECS_CONVERT.toString());
		order.setServiceType(OrderServiceType.AXG.toString());
		order.setPayType(OrderPayType.COD.toString());
		// 重新设置发货方式
		order.setTemplateId(null);

		List<OrderGoods> orderGoodsList = new ArrayList<OrderGoods>();
		for (SfgoOrderGoods sfgoOrderGoods : sfgoOrder.getOrderGoodsList()) {
			OrderGoods orderGoods = new OrderGoods();
			ReflectUtil.copyProperties(sfgoOrderGoods, orderGoods);
			orderGoods.setOrderNo(null);

			orderGoodsList.add(orderGoods);
		}

		order.setGoods(orderGoodsList);

		LogisticsOrderExt orderExt = new LogisticsOrderExt();
		ReflectUtil.copyProperties(sfgoOrder.getLogisticsOrderExt(), orderExt);
		orderExt.setOrderNo(null);
		order.setOrderExt(orderExt);

		logisticsOrderService.add(order, user);
	}

	@Override
	public void previewPrint(List<String> orderNoList, Integer printNumber, User user) {
		logisticsOrderService.previewPrint(orderNoList, printNumber, user);
	}

	@Override
	public int solveDuplicateCheck(List<String> orderNos) {
		return logisticsOrderService.solveDuplicateCheck(orderNos);
	}
}
