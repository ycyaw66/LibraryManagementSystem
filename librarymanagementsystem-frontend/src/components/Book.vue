<template>
    <el-scrollbar height="100%" style="width: 100%; height: 100%; ">
        <!-- 标题 -->
        <div style="margin-top: 20px; margin-left: 40px; font-size: 2em; font-weight: bold; ">图书管理</div>  

        <!-- 查询框第一行 -->
        <div style="margin-left: 20px; padding-top:5vh;">
            <el-input v-model="this.toQueryInfo.title" style="display:inline; margin-left: 20px;" placeholder="书名"></el-input>
            <el-input v-model="this.toQueryInfo.category" style="display:inline; margin-left: 20px;" placeholder="类别"></el-input>
            <el-input v-model="this.toQueryInfo.author" style="display:inline; margin-left: 20px;" placeholder="作者"></el-input>
            <el-input v-model="this.toQueryInfo.press" style="display:inline; margin-left: 20px;" placeholder="出版社"></el-input>
        </div>

        <!-- 查询框第二行 -->
        <div style="margin-left: 20px; padding-top:1vh;">
            <el-input v-model="this.toQueryInfo.minPublishYear" style="display:inline; margin-left: 20px; margin-top: 20px;" placeholder="最小出版年份"></el-input> --
            <el-input v-model="this.toQueryInfo.maxPublishYear" style="display:inline; margin-left: 0; margin-top: 20px;" placeholder="最大出版年份"></el-input>
            <el-input v-model="this.toQueryInfo.minPrice" style="display:inline; margin-left: 20px; margin-top: 20px;" placeholder="最小价格"></el-input> --
            <el-input v-model="this.toQueryInfo.maxPrice" style="display:inline; margin-left: 0; margin-top: 20px;" placeholder="最大价格"></el-input>

            <el-button style="margin-left: 20px;" type="primary" @click="QueryBooks">查询</el-button>
        </div>
        
        <!-- 结果表格 -->
        <el-table v-if="isShow" :data="fitlerTableData" height="600"
            :default-sort="{ prop: 'bookID', order: 'ascending' }" :table-layout="'auto'"
            style="width: 100%; margin-left: 50px; margin-top: 30px; margin-right: 50px; max-width: 80vw;">
            <el-table-column prop="bookID" label="图书编号" sortable />
            <el-table-column prop="title" label="书名" sortable />
            <el-table-column prop="author" label="作者" sortable />
            <el-table-column prop="press" label="出版社" sortable />
            <el-table-column prop="publishYear" label="出版年份" sortable />
            <el-table-column prop="price" label="价格" sortable />
            <el-table-column prop="stock" label="库存" sortable />
        </el-table>
        
        
    </el-scrollbar>
</template>

<script>
import axios from 'axios';

export default {
    data() {
        return {
            isShow: true, // 结果表格展示状态
            tableData: [{ // 列表项
                category: 'test',
                title: 'test',
                press: 'test',
                minPublishYear: 0,
                maxPublishYear: 0,
                author: 'test',
                minPrice: 0,
                maxPrice: 0
            }],
            newBookInfo: { // 待新建图书信息
                category: '',
                title: '',
                press: '',
                publishYear: 0,
                author: '',
                price: 0.0
            },
            toQueryInfo: { // 待查询图书信息
                category: '',
                title: '',
                press: '',
                minPublishYear: '',
                maxPublishYear: '',
                author: '',
                minPrice: '',
                maxPrice: ''
            },
        }
    },
    methods: {
        
    }
}
</script>