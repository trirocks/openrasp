//Copyright 2017-2018 Baidu Inc.
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http: //www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.

package logs

import (
	"fmt"
	"crypto/md5"
	"encoding/json"
)

type RaspLog struct {
	content string
}

var (
	PolicyIndexName      = "openrasp-policy-alarm"
	AliasPolicyIndexName = "real-openrasp-policy-alarm"
	PolicyEsMapping      = `
	{
		"mappings": {
			"_default_": {
				"_all": {
					"enabled": false
				},
				"properties": {
					"@timestamp":{
						"type":"date"
         			},
					"event_type": {
						"type": "keyword",
						"ignore_above": 256
					},
					"server_hostname": {
						"type": "keyword",
						"ignore_above": 256
					},
					"server_type": {
						"type": "keyword",
						"ignore_above": 64
					},
					"server_nic": {
						"type": "nested",
						"properties": {
							"name": {
								"type": "keyword",
								"ignore_above": 256
							},
							"ip": {
								"type": "ip"
							}
						}
					},
					"app_id": {
						"type": "keyword",
						"ignore_above": 256
					},
					"rasp_id": {
						"type": "keyword",
						"ignore_above": 256
					},
					"local_ip": {
						"type": "ip"
					},
					"event_time": {
						"type": "date"
					},
					"stack_trace": {
						"type": "keyword"
					},
					"policy_id": {
						"type": "long"
					},
					"message": {
						"type": "keyword"
					},
					"stack_md5": {
						"type": "keyword",
						"ignore_above": 64
					},
					"param": {
						"type": "object",
						"enabled":"false"
					}
				}
			}
		}
	}
`
)

func AddPolicyAlarm(alarm map[string]interface{}) error {
	if stack, ok := alarm["stack_trace"]; ok && stack != nil {
		_, ok = stack.(string)
		if ok {
			alarm["stack_md5"] = fmt.Sprintf("%x", md5.Sum([]byte(stack.(string))))
		}
	}
	content, err := json.Marshal(alarm)
	if err == nil {
		AddAlarmFunc(AttackAlarmType, content)
	}
	return err
}
