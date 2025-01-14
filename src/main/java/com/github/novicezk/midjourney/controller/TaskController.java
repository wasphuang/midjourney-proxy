package com.github.novicezk.midjourney.controller;

import cn.hutool.core.comparator.CompareUtil;
import com.github.novicezk.midjourney.aliyun.OSSFileClient;
import com.github.novicezk.midjourney.dto.TaskConditionDTO;
import com.github.novicezk.midjourney.service.TaskStoreService;
import com.github.novicezk.midjourney.support.Task;
import com.github.novicezk.midjourney.support.TaskQueueHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Api(tags = "任务查询")
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {
	private final TaskStoreService taskStoreService;
	private final TaskQueueHelper taskQueueHelper;
	@Autowired
	private OSSFileClient ossFileClient;

	@ApiOperation(value = "查询所有任务")
	@GetMapping("/list")
	public List<Task> list() {
		return this.taskStoreService.list().stream()
				.sorted((t1, t2) -> CompareUtil.compare(t2.getSubmitTime(), t1.getSubmitTime()))
				.toList();
	}

	@ApiOperation(value = "指定ID获取任务")
	@GetMapping("/{id}/fetch")
	public Task fetch(@ApiParam(value = "任务ID") @PathVariable String id) {
		Task task  = this.taskStoreService.get(id);
		if(task != null){
			String imageURL = task.getImageUrl();
			if(imageURL != null && imageURL.indexOf("threeing.cn") == -1){//处理图片地址
				imageURL = ossFileClient.uploadImage(imageURL,id);
				if(imageURL !=null){
					task.setImageUrl(imageURL);
				}
			}

			String prompt = task.getPrompt();
			if(prompt != null && prompt.startsWith("https") && prompt.indexOf("threeing.cn") == -1 ){//有垫图
				String[] dataURL = prompt.split(" ");
				for (String s : dataURL) {
					if(s.startsWith("https")){
						String _diantuURL = ossFileClient.uploadImage(s,id);
						if(_diantuURL !=null){
							task.setPrompt(prompt.replace(s,_diantuURL));
							task.setPromptEn(task.getPromptEn().replace(s,_diantuURL));
							task.setDescription(task.getDescription().replace(s,_diantuURL));
						}
					}
				}
			}
		}
		return task;
	}

	@ApiOperation(value = "查询任务队列")
	@GetMapping("/queue")
	public List<Task> queue() {
		Set<String> queueTaskIds = this.taskQueueHelper.getQueueTaskIds();
		return queueTaskIds.stream().map(this.taskStoreService::get).filter(Objects::nonNull)
				.sorted(Comparator.comparing(Task::getSubmitTime))
				.toList();
	}

	@ApiOperation(value = "根据条件查询任务")
	@PostMapping("/list-by-condition")
	public List<Task> listByCondition(@RequestBody TaskConditionDTO conditionDTO) {
		if (conditionDTO.getIds() == null) {
			return Collections.emptyList();
		}
		return conditionDTO.getIds().stream().map(this.taskStoreService::get).filter(Objects::nonNull).toList();
	}

}
